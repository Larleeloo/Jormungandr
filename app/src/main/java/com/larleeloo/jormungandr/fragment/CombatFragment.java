package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.activity.GameActivity;
import com.larleeloo.jormungandr.adapter.ActionAdapter;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.engine.CombatEngine;
import com.larleeloo.jormungandr.engine.LootGenerator;
import com.larleeloo.jormungandr.engine.PlayerLevelManager;
import com.larleeloo.jormungandr.model.ActionType;
import com.larleeloo.jormungandr.model.CombatCreature;
import com.larleeloo.jormungandr.model.CreatureDef;
import com.larleeloo.jormungandr.model.EquipmentSlot;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.SeededRandom;
import com.larleeloo.jormungandr.view.CombatCanvasView;

import java.util.ArrayList;
import java.util.List;

public class CombatFragment extends Fragment implements ActionAdapter.OnActionClickListener {

    private static final String ARG_CREATURE_ID = "creatureDefId";
    private static final String ARG_LEVEL = "level";
    private static final String ARG_HP = "hp";

    private CombatCanvasView combatCanvas;
    private RecyclerView actionList;
    private TextView actionPrompt;
    private Button btnFlee;

    private CombatEngine combatEngine;
    private CombatCreature combatCreature;
    private boolean playerTurnActive = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static CombatFragment newInstance(String creatureDefId, int level, int hp) {
        CombatFragment fragment = new CombatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CREATURE_ID, creatureDefId);
        args.putInt(ARG_LEVEL, level);
        args.putInt(ARG_HP, hp);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_combat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        combatCanvas = view.findViewById(R.id.combat_canvas);
        actionList = view.findViewById(R.id.action_list);
        actionPrompt = view.findViewById(R.id.action_prompt);
        btnFlee = view.findViewById(R.id.btn_flee);

        actionList.setLayoutManager(new LinearLayoutManager(requireContext()));

        GameRepository repo = GameRepository.getInstance(requireContext());
        combatEngine = new CombatEngine(repo.getItemRegistry());

        // Setup creature
        Bundle args = getArguments();
        if (args != null) {
            String creatureDefId = args.getString(ARG_CREATURE_ID);
            int level = args.getInt(ARG_LEVEL, 1);
            int hp = args.getInt(ARG_HP, 20);

            CreatureDef def = repo.getCreatureRegistry().getCreature(creatureDefId);
            if (def != null) {
                double scale = 1.0 + Constants.DIFFICULTY_SCALE_PER_LEVEL * (level - 1);
                combatCreature = new CombatCreature(def, level, scale);
                combatCreature.setCurrentHp(hp);
                combatCreature.setMaxHp(hp);
            }
        }

        Player player = repo.getCurrentPlayer();
        if (player != null && combatCreature != null) {
            combatCanvas.setCombatants(player, combatCreature, player.getCurrentRegion());
            combatCanvas.setMessage("A wild " + combatCreature.getDef().getDisplayName() +
                    " appears! (Lv." + combatCreature.getLevel() + ")");
        }

        buildActionList();

        btnFlee.setOnClickListener(v -> flee());
    }

    private void buildActionList() {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        List<ActionAdapter.CombatAction> actions = new ArrayList<>();

        // Actions from equipped items
        for (EquipmentSlot eq : player.getEquipment()) {
            ItemDef item = repo.getItemRegistry().getItem(eq.getItemId());
            if (item == null) continue;

            for (String actionName : item.getActions()) {
                try {
                    ActionType actionType = ActionType.valueOf(actionName.toUpperCase());
                    if (actionType != ActionType.DROP) {
                        actions.add(new ActionAdapter.CombatAction(item, actionType));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Consumables from inventory (free actions)
        for (InventorySlot slot : player.getInventory()) {
            if (slot.isEmpty()) continue;
            ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
            if (item != null && item.isConsumable()) {
                actions.add(new ActionAdapter.CombatAction(item, ActionType.USE));
            }
        }

        // If no weapons equipped, add a basic "Punch" action
        if (actions.isEmpty() || actions.stream().noneMatch(a -> a.actionType.isCombatAction())) {
            ItemDef punch = new ItemDef();
            punch.setItemId("fists");
            punch.setDisplayName("Fists");
            punch.setDamage(1);
            punch.setPlaceholderColor("#CC9966");
            punch.setPlaceholderShape("circle");
            punch.setRarity("COMMON");
            punch.setType("WEAPON");
            actions.add(0, new ActionAdapter.CombatAction(punch, ActionType.SWING));
        }

        ActionAdapter adapter = new ActionAdapter(actions, this);
        actionList.setAdapter(adapter);
    }

    @Override
    public void onActionClick(ActionAdapter.CombatAction action) {
        if (!playerTurnActive || combatCreature == null) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        CombatEngine.CombatResult result;

        if (action.actionType == ActionType.USE) {
            // Free action - use consumable, don't end turn
            result = combatEngine.useConsumable(player, combatCreature, action.item.getItemId());
            combatCanvas.setMessage(result.message);
            combatCanvas.updateState();
            buildActionList(); // Refresh since item consumed

            GameActivity activity = (GameActivity) getActivity();
            if (activity != null) activity.updateHud();
            return;
        }

        // Combat action - ends player turn
        playerTurnActive = false;
        result = combatEngine.playerAttack(player, combatCreature, action.item.getItemId(),
                action.actionType);

        combatCanvas.setMessage(result.message);
        combatCanvas.updateState();

        if (result.combatOver) {
            handleCombatEnd(result.playerWon);
            return;
        }

        // Creature's turn after a delay
        actionPrompt.setText("Enemy's turn...");
        handler.postDelayed(() -> {
            CombatEngine.CombatResult creatureResult = combatEngine.creatureTurn(player, combatCreature);
            combatCanvas.setMessage(creatureResult.message);
            combatCanvas.updateState();

            GameActivity activity = (GameActivity) getActivity();
            if (activity != null) activity.updateHud();

            if (creatureResult.combatOver) {
                handleCombatEnd(creatureResult.playerWon);
            } else {
                playerTurnActive = true;
                actionPrompt.setText("Choose an action:");
                buildActionList();
            }
        }, 1500);
    }

    private void handleCombatEnd(boolean victory) {
        playerTurnActive = false;
        btnFlee.setVisibility(View.GONE);
        actionList.setVisibility(View.GONE);

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();

        if (victory && player != null && combatCreature != null) {
            // Award XP
            int xp = combatCreature.getDef().getXpReward();
            boolean leveledUp = PlayerLevelManager.awardXp(player, xp);
            String msg = "Victory! +" + xp + " XP";
            if (leveledUp) msg += " LEVEL UP! You are now level " + player.getLevel() + "!";

            // Generate and award loot
            LootGenerator lootGen = new LootGenerator(repo.getItemRegistry());
            SeededRandom rng = new SeededRandom(System.currentTimeMillis());
            List<InventorySlot> loot = lootGen.generateCreatureLoot(rng,
                    combatCreature.getDef().getLootTable(), combatCreature.getLevel());

            StringBuilder lootMsg = new StringBuilder();
            for (InventorySlot drop : loot) {
                ItemDef item = repo.getItemRegistry().getItem(drop.getItemId());
                String name = item != null ? item.getDisplayName() : drop.getItemId();
                player.addItemToInventory(drop.getItemId(), drop.getQuantity());
                lootMsg.append(name).append(" x").append(drop.getQuantity()).append(", ");
            }

            if (lootMsg.length() > 0) {
                msg += "\nLoot: " + lootMsg.substring(0, lootMsg.length() - 2);
            }

            // Mark creature as dead in room
            Room room = repo.getCurrentRoom();
            if (room != null) {
                for (RoomObject obj : room.getObjects()) {
                    if ("creature".equals(obj.getType()) && obj.isAlive()) {
                        obj.setAlive(false);
                        break;
                    }
                }
                repo.saveCurrentRoom();
            }

            combatCanvas.setMessage(msg);
            actionPrompt.setText("Tap to continue...");

            repo.savePlayer();
        } else {
            combatCanvas.setMessage("You have been defeated...");
            actionPrompt.setText("Returning to hub...");
        }

        combatEngine.clearBuffs();

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();

        // Return to room after delay
        handler.postDelayed(() -> {
            GameActivity act = (GameActivity) getActivity();
            if (act != null) act.returnFromCombat(victory);
        }, 3000);
    }

    private void flee() {
        GameActivity activity = (GameActivity) getActivity();
        if (activity == null) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Room room = repo.getCurrentRoom();

        // Navigate back through the back door
        if (room != null && room.hasDoor(com.larleeloo.jormungandr.model.Direction.BACK)) {
            String backRoom = room.getDoorTarget(com.larleeloo.jormungandr.model.Direction.BACK);
            combatEngine.clearBuffs();
            activity.navigateToRoom(backRoom);
        } else {
            combatEngine.clearBuffs();
            activity.returnFromCombat(false);
        }
    }
}
