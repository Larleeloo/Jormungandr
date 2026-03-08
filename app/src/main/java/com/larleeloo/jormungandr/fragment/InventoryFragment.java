package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.activity.GameActivity;
import com.larleeloo.jormungandr.adapter.InventoryAdapter;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.EquipmentSlot;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;
import com.larleeloo.jormungandr.view.CharacterSilhouetteView;

public class InventoryFragment extends Fragment implements InventoryAdapter.OnSlotClickListener {

    private RecyclerView inventoryGrid;
    private InventoryAdapter adapter;
    private LinearLayout itemDetailPanel;
    private TextView itemName, itemDescription, itemStats, goldDisplay;
    private Button btnEquip, btnUse, btnDrop;
    private TextView eqMainHand, eqOffHand, eqHead, eqChest, eqLegs, eqFeet, eqAcc1, eqAcc2;
    private CharacterSilhouetteView characterSilhouette;
    private int selectedSlotIndex = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inventoryGrid = view.findViewById(R.id.inventory_grid);
        itemDetailPanel = view.findViewById(R.id.item_detail_panel);
        itemName = view.findViewById(R.id.item_name);
        itemDescription = view.findViewById(R.id.item_description);
        itemStats = view.findViewById(R.id.item_stats);
        goldDisplay = view.findViewById(R.id.gold_display);
        btnEquip = view.findViewById(R.id.btn_equip);
        btnUse = view.findViewById(R.id.btn_use);
        btnDrop = view.findViewById(R.id.btn_drop);

        eqMainHand = view.findViewById(R.id.equipment_main_hand);
        eqOffHand = view.findViewById(R.id.equipment_off_hand);
        eqHead = view.findViewById(R.id.equipment_head);
        eqChest = view.findViewById(R.id.equipment_chest);
        eqLegs = view.findViewById(R.id.equipment_legs);
        eqFeet = view.findViewById(R.id.equipment_feet);
        eqAcc1 = view.findViewById(R.id.equipment_acc1);
        eqAcc2 = view.findViewById(R.id.equipment_acc2);
        characterSilhouette = view.findViewById(R.id.character_silhouette);

        inventoryGrid.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player != null) {
            adapter = new InventoryAdapter(player.getInventory(), repo.getItemRegistry(), this);
            inventoryGrid.setAdapter(adapter);
            goldDisplay.setText("Gold: " + player.getGold());
            updateEquipmentDisplay(player, repo);
        }

        btnEquip.setOnClickListener(v -> equipSelectedItem());
        btnUse.setOnClickListener(v -> useSelectedItem());
        btnDrop.setOnClickListener(v -> dropSelectedItem());

        // Highlight silhouette during drag
        View equipArea = view.findViewById(R.id.character_equip_area);
        equipArea.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    characterSilhouette.setHighlight(true);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    characterSilhouette.setHighlight(false);
                    return true;
            }
            return true;
        });

        // Setup drag-and-drop on equipment slots
        setupEquipDragTarget(eqHead, EquipmentSlot.HEAD);
        setupEquipDragTarget(eqChest, EquipmentSlot.CHEST);
        setupEquipDragTarget(eqLegs, EquipmentSlot.LEGS);
        setupEquipDragTarget(eqFeet, EquipmentSlot.FEET);
        setupEquipDragTarget(eqMainHand, EquipmentSlot.MAIN_HAND);
        setupEquipDragTarget(eqOffHand, EquipmentSlot.OFF_HAND);
        setupEquipDragTarget(eqAcc1, EquipmentSlot.ACCESSORY_1);
        setupEquipDragTarget(eqAcc2, EquipmentSlot.ACCESSORY_2);
    }

    private void setupEquipSlotTap(TextView slotView, String equipSlotName) {
        slotView.setOnClickListener(v -> {
            GameRepository repo = GameRepository.getInstance(requireContext());
            Player player = repo.getCurrentPlayer();
            if (player == null) return;

            EquipmentSlot eq = player.getEquipped(equipSlotName);
            if (eq == null || eq.isEmpty()) return;

            // Unequip: move back to inventory
            boolean added = player.addItemToInventory(eq.getItemId(), 1);
            if (!added) {
                Toast.makeText(requireContext(), "Inventory full!", Toast.LENGTH_SHORT).show();
                return;
            }
            player.getEquipment().remove(eq);
            player.ensureInventoryCapacity();
            repo.savePlayer();
            adapter.notifyDataSetChanged();
            updateEquipmentDisplay(player, repo);

            Toast.makeText(requireContext(), "Unequipped", Toast.LENGTH_SHORT).show();
            GameActivity activity = (GameActivity) getActivity();
            if (activity != null) activity.updateHud();
        });
    }

    private void setupEquipDragTarget(TextView slotView, String equipSlotName) {
        setupEquipSlotTap(slotView, equipSlotName);
        slotView.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundResource(R.drawable.inventory_slot_selected);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundResource(R.drawable.inventory_slot_background);
                    return true;
                case DragEvent.ACTION_DROP:
                    v.setBackgroundResource(R.drawable.inventory_slot_background);
                    Object localState = event.getLocalState();
                    if (localState instanceof InventorySlot) {
                        InventorySlot draggedSlot = (InventorySlot) localState;
                        handleDragEquip(draggedSlot, equipSlotName);
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundResource(R.drawable.inventory_slot_background);
                    return true;
            }
            return false;
        });
    }

    private void handleDragEquip(InventorySlot draggedSlot, String targetEquipSlot) {
        if (draggedSlot.isEmpty()) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        ItemDef item = repo.getItemRegistry().getItem(draggedSlot.getItemId());
        if (item == null || !item.isEquippable()) {
            Toast.makeText(requireContext(), "This item can't be equipped here", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if item matches the target slot
        if (!item.getEquipSlot().equals(targetEquipSlot)) {
            Toast.makeText(requireContext(), item.getDisplayName() + " goes in " + item.getEquipSlot(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Unequip existing item in that slot
        EquipmentSlot existing = player.getEquipped(targetEquipSlot);
        if (existing != null && !existing.isEmpty()) {
            player.addItemToInventory(existing.getItemId(), 1);
            player.getEquipment().remove(existing);
        }

        // Remove from inventory and equip
        draggedSlot.setQuantity(draggedSlot.getQuantity() - 1);
        if (draggedSlot.getQuantity() <= 0) {
            draggedSlot.setItemId(null);
            draggedSlot.setQuantity(0);
        }

        EquipmentSlot newEquip = new EquipmentSlot(item.getItemId(), targetEquipSlot);
        player.getEquipment().add(newEquip);
        player.ensureInventoryCapacity();

        repo.savePlayer();
        adapter.notifyDataSetChanged();
        updateEquipmentDisplay(player, repo);
        itemDetailPanel.setVisibility(View.GONE);

        Toast.makeText(requireContext(), "Equipped " + item.getDisplayName(), Toast.LENGTH_SHORT).show();

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();
    }

    @Override
    public void onSlotClick(int position, InventorySlot slot) {
        selectedSlotIndex = position;

        if (slot.isEmpty()) {
            itemDetailPanel.setVisibility(View.GONE);
            return;
        }

        GameRepository repo = GameRepository.getInstance(requireContext());
        ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
        if (item == null) {
            itemDetailPanel.setVisibility(View.GONE);
            return;
        }

        itemName.setText(item.getDisplayName());
        itemName.setTextColor(item.getRarityEnum().getGlowColor());
        itemDescription.setText(item.getDescription());

        StringBuilder stats = new StringBuilder();
        stats.append(item.getRarityEnum().getDisplayName()).append(" ").append(item.getType());
        if (item.getDamage() > 0) stats.append(" | DMG: ").append(item.getDamage());
        if (item.getDefense() > 0) stats.append(" | DEF: ").append(item.getDefense());
        if (item.getHealAmount() > 0) stats.append(" | HEAL: +").append(item.getHealAmount());
        if (item.getManaRestore() > 0) stats.append(" | MANA: +").append(item.getManaRestore());
        stats.append(" | Qty: ").append(slot.getQuantity());
        itemStats.setText(stats.toString());

        btnEquip.setVisibility(item.isEquippable() ? View.VISIBLE : View.GONE);
        btnUse.setVisibility(item.isUsable() ? View.VISIBLE : View.GONE);

        itemDetailPanel.setVisibility(View.VISIBLE);
    }

    private void equipSelectedItem() {
        if (selectedSlotIndex < 0) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        InventorySlot slot = player.getInventory().get(selectedSlotIndex);
        if (slot.isEmpty()) return;

        ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
        if (item == null || !item.isEquippable()) return;

        // Unequip existing item in that slot
        String equipSlotName = item.getEquipSlot();
        EquipmentSlot existing = player.getEquipped(equipSlotName);
        if (existing != null && !existing.isEmpty()) {
            player.addItemToInventory(existing.getItemId(), 1);
            player.getEquipment().remove(existing);
        }

        // Remove from inventory and equip
        slot.setQuantity(slot.getQuantity() - 1);
        if (slot.getQuantity() <= 0) {
            slot.setItemId(null);
            slot.setQuantity(0);
        }

        EquipmentSlot newEquip = new EquipmentSlot(item.getItemId(), equipSlotName);
        player.getEquipment().add(newEquip);

        // Ensure inventory capacity is updated (e.g., backpack adds slots)
        player.ensureInventoryCapacity();

        repo.savePlayer();
        adapter.notifyDataSetChanged();
        updateEquipmentDisplay(player, repo);
        itemDetailPanel.setVisibility(View.GONE);

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();
    }

    private void useSelectedItem() {
        if (selectedSlotIndex < 0) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        InventorySlot slot = player.getInventory().get(selectedSlotIndex);
        if (slot.isEmpty()) return;

        ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
        if (item == null || !item.isUsable()) return;

        // Torch: reveal hidden objects in the current room
        if ("torch".equals(item.getItemId())) {
            Room room = repo.getCurrentRoom();
            if (room == null) {
                Toast.makeText(requireContext(), "No room to search!", Toast.LENGTH_SHORT).show();
                return;
            }
            int revealed = 0;
            for (RoomObject obj : room.getObjects()) {
                if (obj.isHidden()) {
                    obj.setHidden(false);
                    revealed++;
                }
            }
            // Consume the torch
            slot.setQuantity(slot.getQuantity() - 1);
            if (slot.getQuantity() <= 0) {
                slot.setItemId(null);
                slot.setQuantity(0);
            }
            repo.savePlayer();
            repo.saveCurrentRoom();
            adapter.notifyDataSetChanged();
            itemDetailPanel.setVisibility(View.GONE);
            if (revealed > 0) {
                Toast.makeText(requireContext(), "The torch reveals " + revealed + " hidden object(s)!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "The torch illuminates the room. Nothing hidden here.", Toast.LENGTH_SHORT).show();
            }
            GameActivity activity = (GameActivity) getActivity();
            if (activity != null) activity.updateHud();
            return;
        }

        // Standard consumable effects
        if (item.getHealAmount() > 0) {
            player.setHp(Math.min(player.getMaxHp(), player.getHp() + item.getHealAmount()));
        }
        if (item.getManaRestore() > 0) {
            player.setMana(Math.min(player.getMaxMana(), player.getMana() + item.getManaRestore()));
        }
        if (item.getStaminaRestore() > 0) {
            player.setStamina(Math.min(player.getMaxStamina(), player.getStamina() + item.getStaminaRestore()));
        }

        // Consume
        slot.setQuantity(slot.getQuantity() - 1);
        if (slot.getQuantity() <= 0) {
            slot.setItemId(null);
            slot.setQuantity(0);
        }

        repo.savePlayer();
        adapter.notifyDataSetChanged();
        itemDetailPanel.setVisibility(View.GONE);

        Toast.makeText(requireContext(), "Used " + item.getDisplayName(), Toast.LENGTH_SHORT).show();

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) activity.updateHud();
    }

    private void dropSelectedItem() {
        if (selectedSlotIndex < 0) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        InventorySlot slot = player.getInventory().get(selectedSlotIndex);
        if (slot.isEmpty()) return;

        String itemId = slot.getItemId();
        slot.setItemId(null);
        slot.setQuantity(0);

        player.compactInventory();
        repo.savePlayer();
        selectedSlotIndex = -1;
        adapter.notifyDataSetChanged();
        itemDetailPanel.setVisibility(View.GONE);

        Toast.makeText(requireContext(), "Dropped item", Toast.LENGTH_SHORT).show();
    }

    private void updateEquipmentDisplay(Player player, GameRepository repo) {
        updateEquipSlot(eqMainHand, player, repo, EquipmentSlot.MAIN_HAND, "Main");
        updateEquipSlot(eqOffHand, player, repo, EquipmentSlot.OFF_HAND, "Off");
        updateEquipSlot(eqHead, player, repo, EquipmentSlot.HEAD, "Head");
        updateEquipSlot(eqChest, player, repo, EquipmentSlot.CHEST, "Chest");
        updateEquipSlot(eqLegs, player, repo, EquipmentSlot.LEGS, "Legs");
        updateEquipSlot(eqFeet, player, repo, EquipmentSlot.FEET, "Feet");
        updateEquipSlot(eqAcc1, player, repo, EquipmentSlot.ACCESSORY_1, "Ring");
        updateEquipSlot(eqAcc2, player, repo, EquipmentSlot.ACCESSORY_2, "Neck");
    }

    private void updateEquipSlot(TextView view, Player player, GameRepository repo,
                                  String slotName, String label) {
        EquipmentSlot eq = player.getEquipped(slotName);
        if (eq != null && !eq.isEmpty()) {
            ItemDef item = repo.getItemRegistry().getItem(eq.getItemId());
            if (item != null) {
                // Show abbreviated name that fits: up to 8 chars with rarity color
                String name = item.getDisplayName();
                if (name.length() > 8) name = name.substring(0, 7) + ".";
                view.setText(name);
                view.setTextColor(item.getRarityEnum().getGlowColor());
                return;
            }
        }
        view.setText(label);
        view.setTextColor(0xFFAAAAAA);
    }
}
