package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.activity.GameActivity;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;
import com.larleeloo.jormungandr.view.RoomCanvasView;

import java.util.List;

public class RoomFragment extends Fragment implements RoomCanvasView.RoomInteractionListener {

    private RoomCanvasView roomCanvas;
    private TextView roomMessage;
    private Button btnUseTorch;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        roomCanvas = view.findViewById(R.id.room_canvas);
        roomMessage = view.findViewById(R.id.room_message);
        roomCanvas.setInteractionListener(this);

        btnUseTorch = view.findViewById(R.id.btn_use_torch);
        btnUseTorch.setOnClickListener(v -> useTorch());

        Button btnLeaveNote = view.findViewById(R.id.btn_leave_note);
        btnLeaveNote.setOnClickListener(v -> {
            GameActivity activity = (GameActivity) getActivity();
            if (activity != null) {
                activity.showFragment(new NoteFragment(), "note");
            }
        });

        loadCurrentRoom();
    }

    private void loadCurrentRoom() {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Room room = repo.getCurrentRoom();

        if (room == null) {
            Player player = repo.getCurrentPlayer();
            if (player != null) {
                room = repo.loadOrGenerateRoom(player.getCurrentRoomId());
            }
        }

        if (room != null) {
            roomCanvas.setRoom(room);

            // Show torch button if player has torches and room has hidden objects
            updateTorchButton(room);

            // Check for living creature - auto-enter combat
            if (room.hasLivingCreature()) {
                RoomObject creature = room.getFirstLivingCreature();
                showMessage("A " + creature.getCreatureDefId().replace("_", " ") +
                        " blocks your path! (Tap it to fight)");
            }

            // Waypoint notification
            if (room.isWaypoint()) {
                showMessage("You've found a Waypoint! Bank items, trade, or teleport here.");
            }
        }
    }

    @Override
    public void onDoorTapped(Direction direction, String targetRoomId) {
        GameActivity activity = (GameActivity) getActivity();
        if (activity == null || targetRoomId == null) return;

        // Check if creature is blocking
        GameRepository repo = GameRepository.getInstance(requireContext());
        Room currentRoom = repo.getCurrentRoom();
        if (currentRoom != null && currentRoom.hasLivingCreature()) {
            showMessage("A creature blocks the doors! Defeat it first.");
            return;
        }

        activity.navigateToRoom(targetRoomId);
    }

    @Override
    public void onObjectTapped(RoomObject object) {
        if (object == null) return;

        switch (object.getType()) {
            case "chest":
                handleChestTap(object);
                break;
            case "creature":
                handleCreatureTap(object);
                break;
            case "trap":
                handleTrapTap(object);
                break;
            case "item":
                handleFloorItemTap(object);
                break;
            default:
                showMessage("You examine the " + object.getSpriteId());
                break;
        }
    }

    @Override
    public void onBackgroundTapped(float x, float y) {
        // Dismiss message
        roomMessage.setVisibility(View.GONE);
    }

    private void handleChestTap(RoomObject chest) {
        if (chest.isOpened()) {
            showMessage("This chest is empty.");
            return;
        }

        GameRepository repo = GameRepository.getInstance(requireContext());
        Room room = repo.getCurrentRoom();
        if (room != null && room.hasLivingCreature()) {
            showMessage("Defeat the creature first!");
            return;
        }

        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        // Check for hidden traps near the chest
        if (room != null) {
            for (RoomObject obj : room.getObjects()) {
                if ("trap".equals(obj.getType()) && obj.isHidden() && !obj.isTriggered()) {
                    float dist = Math.abs(obj.getX() - chest.getX()) + Math.abs(obj.getY() - chest.getY());
                    if (dist < 0.2f) {
                        // Wisdom check to avoid trap
                        int wisdom = player.getStats().getWisdom();
                        if (Math.random() > wisdom * 0.08) { // 8% dodge per wisdom point
                            obj.setTriggered(true);
                            obj.setHidden(false);
                            int trapDmg = obj.getDamage();
                            player.setHp(Math.max(1, player.getHp() - trapDmg));
                            showMessage("A hidden " + obj.getTrapType() + " trap! -" + trapDmg + " HP");
                            repo.savePlayer();
                            repo.saveCurrentRoom();
                            roomCanvas.renderRoom();
                            GameActivity trapActivity = (GameActivity) getActivity();
                            if (trapActivity != null) trapActivity.updateHud();
                        } else {
                            obj.setHidden(false);
                            showMessage("Your wisdom revealed a hidden trap! You avoid it.");
                            repo.saveCurrentRoom();
                            roomCanvas.renderRoom();
                        }
                    }
                }
            }
        }

        chest.setOpened(true);
        List<InventorySlot> contents = chest.getInventory();

        if (contents == null || contents.isEmpty()) {
            showMessage("The chest is empty!");
        } else {
            StringBuilder sb = new StringBuilder("Found: ");
            int floorIdx = room != null ? room.getObjects().size() : 0;
            for (InventorySlot slot : contents) {
                ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
                String name = item != null ? item.getDisplayName() : slot.getItemId();
                boolean added = player.addItemToInventory(slot.getItemId(), slot.getQuantity());
                sb.append(name).append(" x").append(slot.getQuantity());
                if (!added) {
                    sb.append(" (dropped!)");
                    // Drop overflow items on the floor
                    if (room != null) {
                        float fx = chest.getX() + (float)(Math.random() * 0.1 - 0.05);
                        float fy = chest.getY() + 0.1f + (float)(Math.random() * 0.05);
                        room.getObjects().add(RoomObject.createFloorItem(
                                "overflow_" + floorIdx++, slot.getItemId(), slot.getQuantity(), fx, fy));
                    }
                }
                sb.append(", ");
            }
            showMessage(sb.substring(0, sb.length() - 2));
            repo.savePlayer();
        }

        repo.saveCurrentRoom();
        roomCanvas.renderRoom();

        // Update HUD
        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();
    }

    private void handleCreatureTap(RoomObject creature) {
        if (!creature.isAlive()) {
            showMessage("The creature is already defeated.");
            return;
        }

        GameActivity activity = (GameActivity) getActivity();
        if (activity == null) return;

        activity.startCombat(creature.getCreatureDefId(), creature.getLevel(), creature.getHp());
    }

    private void handleTrapTap(RoomObject trap) {
        if (trap.isTriggered()) {
            showMessage("A triggered trap. Nothing dangerous now.");
            return;
        }

        trap.setTriggered(true);
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();

        if (player != null) {
            int damage = trap.getDamage();
            player.setHp(Math.max(1, player.getHp() - damage));
            showMessage("You triggered a " + trap.getTrapType() + " trap! -" + damage + " HP");
            repo.savePlayer();
        }

        repo.saveCurrentRoom();
        roomCanvas.renderRoom();

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();
    }

    private void handleFloorItemTap(RoomObject floorItem) {
        if (floorItem.getQuantity() <= 0) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        ItemDef item = repo.getItemRegistry().getItem(floorItem.getItemId());
        String name = item != null ? item.getDisplayName() : floorItem.getItemId();

        boolean added = player.addItemToInventory(floorItem.getItemId(), floorItem.getQuantity());
        if (added) {
            showMessage("Picked up " + name + " x" + floorItem.getQuantity());
            floorItem.setQuantity(0);
        } else {
            showMessage("Inventory full! Can't pick up " + name);
        }

        repo.savePlayer();
        repo.saveCurrentRoom();
        roomCanvas.renderRoom();
    }

    private void updateTorchButton(Room room) {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        boolean hasHidden = false;
        for (RoomObject obj : room.getObjects()) {
            if (obj.isHidden()) { hasHidden = true; break; }
        }
        boolean hasTorch = player != null && player.countItem("torch") > 0;
        btnUseTorch.setVisibility(hasTorch && hasHidden ? View.VISIBLE : View.GONE);
        if (hasTorch && hasHidden) {
            btnUseTorch.setText("Torch (" + player.countItem("torch") + ")");
        }
    }

    private void useTorch() {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        Room room = repo.getCurrentRoom();
        if (player == null || room == null) return;

        if (player.countItem("torch") <= 0) {
            showMessage("You have no torches!");
            return;
        }

        player.removeItemFromInventory("torch", 1);

        int revealed = 0;
        for (RoomObject obj : room.getObjects()) {
            if (obj.isHidden()) {
                obj.setHidden(false);
                revealed++;
            }
        }

        repo.savePlayer();
        repo.saveCurrentRoom();
        roomCanvas.renderRoom();
        updateTorchButton(room);

        if (revealed > 0) {
            showMessage("The torch reveals " + revealed + " hidden object(s)!");
        } else {
            showMessage("The torch illuminates the room. Nothing hidden here.");
        }

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();
    }

    private void showMessage(String message) {
        roomMessage.setText(message);
        roomMessage.setVisibility(View.VISIBLE);

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> roomMessage.setVisibility(View.GONE), 4000);
    }
}
