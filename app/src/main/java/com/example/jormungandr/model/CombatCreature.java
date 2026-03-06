package com.example.jormungandr.model;

import java.util.ArrayList;
import java.util.List;

public class CombatCreature {
    private CreatureDef def;
    private int level;
    private int currentHp;
    private int maxHp;
    private int scaledAttack;
    private int scaledDefense;
    private List<BuffEffect> activeBuffs;

    public CombatCreature() {
        this.activeBuffs = new ArrayList<>();
    }

    public CombatCreature(CreatureDef def, int level, double scaleFactor) {
        this();
        this.def = def;
        this.level = level;
        this.maxHp = (int)(def.getBaseHp() * scaleFactor);
        this.currentHp = this.maxHp;
        this.scaledAttack = (int)(def.getBaseAttack() * scaleFactor);
        this.scaledDefense = (int)(def.getBaseDefense() * scaleFactor);
    }

    public boolean isDead() {
        return currentHp <= 0;
    }

    public void takeDamage(int amount) {
        int effectiveDefense = getEffectiveDefense();
        int actualDamage = Math.max(1, amount - effectiveDefense);
        currentHp = Math.max(0, currentHp - actualDamage);
    }

    public int getEffectiveAttack() {
        int bonus = 0;
        for (BuffEffect buff : activeBuffs) {
            if ("attack".equals(buff.getStat())) bonus += buff.getModifier();
        }
        return scaledAttack + bonus;
    }

    public int getEffectiveDefense() {
        int bonus = 0;
        for (BuffEffect buff : activeBuffs) {
            if ("defense".equals(buff.getStat())) bonus += buff.getModifier();
        }
        return scaledDefense + bonus;
    }

    public void tickBuffs() {
        for (BuffEffect buff : activeBuffs) buff.tick();
        activeBuffs.removeIf(BuffEffect::isExpired);
    }

    public CreatureDef getDef() { return def; }
    public void setDef(CreatureDef def) { this.def = def; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getScaledAttack() { return scaledAttack; }
    public void setScaledAttack(int scaledAttack) { this.scaledAttack = scaledAttack; }
    public int getScaledDefense() { return scaledDefense; }
    public void setScaledDefense(int scaledDefense) { this.scaledDefense = scaledDefense; }
    public List<BuffEffect> getActiveBuffs() { return activeBuffs; }
    public void setActiveBuffs(List<BuffEffect> activeBuffs) { this.activeBuffs = activeBuffs; }
}
