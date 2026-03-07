package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        if (currentRoom != null && currentRoom.hasLivingCreature() && direction != Direction.BACK) {
            showMessage("A creature blocks this door! Defeat it or go BACK.");
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
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        chest.setOpened(true);
        List<InventorySlot> contents = chest.getInventory();

        if (contents == null || contents.isEmpty()) {
            showMessage("The chest is empty!");
        } else {
            StringBuilder sb = new StringBuilder("Found: ");
            for (InventorySlot slot : contents) {
                ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
                String name = item != null ? item.getDisplayName() : slot.getItemId();
                boolean added = player.addItemToInventory(slot.getItemId(), slot.getQuantity());
                sb.append(name).append(" x").append(slot.getQuantity());
                if (!added) sb.append(" (full!)");
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

    private void showMessage(String message) {
        roomMessage.setText(message);
        roomMessage.setVisibility(View.VISIBLE);

        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> roomMessage.setVisibility(View.GONE), 4000);
    }
}
