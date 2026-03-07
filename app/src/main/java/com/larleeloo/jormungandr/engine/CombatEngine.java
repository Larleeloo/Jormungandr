package com.larleeloo.jormungandr.engine;

import com.larleeloo.jormungandr.model.ActionType;
import com.larleeloo.jormungandr.model.BuffEffect;
import com.larleeloo.jormungandr.model.CombatCreature;
import com.larleeloo.jormungandr.model.EquipmentSlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.util.FormulaHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stateless turn processor for Pokemon-style combat.
 * Player turn: unlimited free buff/potion use, then one combat action.
 * Creature turn: weighted random ability selection.
 */
public class CombatEngine {

    public static class CombatResult {
        public final String message;
        public final int damageDealt;
        public final boolean playerTurn;
        public final boolean combatOver;
        public final boolean playerWon;

        public CombatResult(String message, int damageDealt, boolean playerTurn,
                           boolean combatOver, boolean playerWon) {
            this.message = message;
            this.damageDealt = damageDealt;
            this.playerTurn = playerTurn;
            this.combatOver = combatOver;
            this.playerWon = playerWon;
        }
    }

    private final ItemRegistry itemRegistry;
    private final Random random = new Random();
    private final List<BuffEffect> playerBuffs = new ArrayList<>();

    public CombatEngine(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /**
     * Execute a player combat action (swing, throw, shoot, cast, block).
     */
    public CombatResult playerAttack(Player player, CombatCreature creature,
                                      String itemId, ActionType action) {
        ItemDef item = itemRegistry.getItem(itemId);
        if (item == null) {
            return new CombatResult("Invalid item!", 0, true, false, false);
        }

        int damage = calculatePlayerDamage(player, item, action, creature);

        switch (action) {
            case SWING:
            case SHOOT:
            case CAST:
                creature.setCurrentHp(Math.max(0, creature.getCurrentHp() - damage));
                String msg = "You " + action.getDisplayName().toLowerCase() + " with " +
                        item.getDisplayName() + " for " + damage + " damage!";

                if (creature.isDead()) {
                    return new CombatResult(msg + " The " + creature.getDef().getDisplayName() +
                            " is defeated!", damage, true, true, true);
                }
                return new CombatResult(msg, damage, true, false, false);

            case THROW:
                creature.setCurrentHp(Math.max(0, creature.getCurrentHp() - (int)(damage * 1.5)));
                // Throwing consumes the item
                player.removeItemFromInventory(itemId, 1);
                int throwDmg = (int)(damage * 1.5);
                String throwMsg = "You throw " + item.getDisplayName() + " for " + throwDmg + " damage!";
                if (creature.isDead()) {
                    return new CombatResult(throwMsg + " The " + creature.getDef().getDisplayName() +
                            " is defeated!", throwDmg, true, true, true);
                }
                return new CombatResult(throwMsg, throwDmg, true, false, false);

            case BLOCK:
                int blocked = item.getDefense() + player.getStats().getConstitution();
                String blockMsg = "You raise your " + item.getDisplayName() + " blocking " +
                        blocked + " damage next hit!";
                playerBuffs.add(new BuffEffect("Block", "defense", blocked, 1, itemId));
                return new CombatResult(blockMsg, 0, true, false, false);

            default:
                return new CombatResult("Nothing happens.", 0, true, false, false);
        }
    }

    /**
     * Use a consumable (potion, food, scroll) during combat. This is a FREE action.
     */
    public CombatResult useConsumable(Player player, CombatCreature creature, String itemId) {
        ItemDef item = itemRegistry.getItem(itemId);
        if (item == null) return new CombatResult("Invalid item!", 0, true, false, false);

        StringBuilder msg = new StringBuilder("You use " + item.getDisplayName() + ". ");

        if (item.getHealAmount() > 0) {
            player.setHp(Math.min(player.getMaxHp(), player.getHp() + item.getHealAmount()));
            msg.append("+").append(item.getHealAmount()).append(" HP. ");
        }
        if (item.getManaRestore() > 0) {
            player.setMana(Math.min(player.getMaxMana(), player.getMana() + item.getManaRestore()));
            msg.append("+").append(item.getManaRestore()).append(" Mana. ");
        }
        if (item.getStaminaRestore() > 0) {
            player.setStamina(Math.min(player.getMaxStamina(), player.getStamina() + item.getStaminaRestore()));
            msg.append("+").append(item.getStaminaRestore()).append(" Stamina. ");
        }

        // Check for attack buffs (potions of strength, etc.)
        if (item.getDamage() > 0) {
            playerBuffs.add(new BuffEffect(item.getDisplayName(), "attack",
                    item.getDamage(), 3, itemId));
            msg.append("+").append(item.getDamage()).append(" ATK for 3 turns. ");
        }

        // Check for debuffs on creature
        if (item.getDebuffRegions() != null && !item.getDebuffRegions().isEmpty()) {
            if (item.getDebuffRegions().contains(creature.getDef().getRegion())) {
                creature.getActiveBuffs().add(new BuffEffect(item.getDisplayName(), "defense",
                        -3, 3, itemId));
                msg.append("Enemy weakened! ");
            }
        }

        // Consume the item
        player.removeItemFromInventory(itemId, 1);

        return new CombatResult(msg.toString().trim(), 0, true, false, false);
    }

    /**
     * Execute the creature's turn.
     */
    public CombatResult creatureTurn(Player player, CombatCreature creature) {
        int creatureAttack = creature.getEffectiveAttack();

        // Calculate player defense
        int playerDefense = calculatePlayerDefense(player);

        // Check for block buff
        int blockBonus = 0;
        for (int i = playerBuffs.size() - 1; i >= 0; i--) {
            BuffEffect buff = playerBuffs.get(i);
            if ("defense".equals(buff.getStat())) {
                blockBonus += buff.getModifier();
            }
        }

        int totalDefense = playerDefense + blockBonus;
        int damage = Math.max(1, creatureAttack - totalDefense);

        // Check if shield breaks
        boolean shieldBroken = false;
        if (blockBonus > 0 && damage > blockBonus) {
            // Shield/block absorbed some but breaks
            shieldBroken = true;
            playerBuffs.removeIf(b -> "defense".equals(b.getStat()));
        }

        player.setHp(Math.max(0, player.getHp() - damage));

        // Tick all buffs
        for (BuffEffect buff : playerBuffs) buff.tick();
        playerBuffs.removeIf(BuffEffect::isExpired);
        creature.tickBuffs();

        String ability = "attacks";
        if (creature.getDef().getAbilities() != null && !creature.getDef().getAbilities().isEmpty()) {
            ability = creature.getDef().getAbilities().get(
                    random.nextInt(creature.getDef().getAbilities().size()));
            ability = ability.replace("_", " ");
        }

        StringBuilder msg = new StringBuilder();
        msg.append(creature.getDef().getDisplayName()).append(" uses ").append(ability);
        msg.append(" for ").append(damage).append(" damage!");

        if (shieldBroken) {
            msg.append(" Your shield breaks!");
        }

        if (!player.isAlive()) {
            return new CombatResult(msg + " You have been defeated!",
                    damage, false, true, false);
        }

        return new CombatResult(msg.toString(), damage, false, false, false);
    }

    private int calculatePlayerDamage(Player player, ItemDef weapon, ActionType action,
                                       CombatCreature creature) {
        int baseDamage = weapon.getDamage();
        int strength = player.getStats().getStrength();
        int level = player.getLevel();

        int damage = FormulaHelper.calculateAttackDamage(baseDamage, strength, level);

        // Apply attack buffs
        for (BuffEffect buff : playerBuffs) {
            if ("attack".equals(buff.getStat())) {
                damage += buff.getModifier();
            }
        }

        // Intelligence bonus for cast actions
        if (action == ActionType.CAST) {
            damage += player.getStats().getIntelligence() * 2;
        }

        // Dexterity bonus for ranged
        if (action == ActionType.SHOOT) {
            damage += player.getStats().getDexterity();
        }

        return Math.max(1, damage);
    }

    private int calculatePlayerDefense(Player player) {
        int totalDefense = 0;
        for (EquipmentSlot eq : player.getEquipment()) {
            ItemDef item = itemRegistry.getItem(eq.getItemId());
            if (item != null) totalDefense += item.getDefense();
        }
        return FormulaHelper.calculateDefense(totalDefense,
                player.getStats().getConstitution(), player.getStats().getDexterity());
    }

    public List<BuffEffect> getPlayerBuffs() {
        return playerBuffs;
    }

    public void clearBuffs() {
        playerBuffs.clear();
    }
}
