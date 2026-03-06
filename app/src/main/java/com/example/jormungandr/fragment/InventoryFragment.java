package com.example.jormungandr.fragment;

import android.os.Bundle;
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

import com.example.jormungandr.R;
import com.example.jormungandr.activity.GameActivity;
import com.example.jormungandr.adapter.InventoryAdapter;
import com.example.jormungandr.data.GameRepository;
import com.example.jormungandr.model.EquipmentSlot;
import com.example.jormungandr.model.InventorySlot;
import com.example.jormungandr.model.ItemDef;
import com.example.jormungandr.model.Player;

public class InventoryFragment extends Fragment implements InventoryAdapter.OnSlotClickListener {

    private RecyclerView inventoryGrid;
    private InventoryAdapter adapter;
    private LinearLayout itemDetailPanel;
    private TextView itemName, itemDescription, itemStats, goldDisplay;
    private Button btnEquip, btnUse, btnDrop;
    private TextView eqMainHand, eqOffHand, eqHead, eqChest, eqLegs, eqFeet;
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
        btnUse.setVisibility(item.isConsumable() ? View.VISIBLE : View.GONE);

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
        if (item == null || !item.isConsumable()) return;

        // Apply effects
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

        repo.savePlayer();
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
    }

    private void updateEquipSlot(TextView view, Player player, GameRepository repo,
                                  String slotName, String label) {
        EquipmentSlot eq = player.getEquipped(slotName);
        if (eq != null && !eq.isEmpty()) {
            ItemDef item = repo.getItemRegistry().getItem(eq.getItemId());
            if (item != null) {
                view.setText(item.getDisplayName().length() > 6 ?
                        item.getDisplayName().substring(0, 6) : item.getDisplayName());
                view.setTextColor(item.getRarityEnum().getGlowColor());
                return;
            }
        }
        view.setText(label);
        view.setTextColor(0xFFAAAAAA);
    }
}
