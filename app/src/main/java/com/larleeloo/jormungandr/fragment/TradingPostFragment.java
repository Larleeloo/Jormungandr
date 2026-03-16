package com.larleeloo.jormungandr.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.adapter.TradeListingAdapter;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.TradeListing;

import java.util.ArrayList;
import java.util.List;

public class TradingPostFragment extends Fragment implements TradeListingAdapter.OnTradeActionListener {

    private TextView goldDisplay;
    private Button selectItemBtn;
    private EditText quantityInput;
    private EditText priceInput;
    private TradeListingAdapter adapter;

    private int selectedInventorySlot = -1;
    private String selectedItemId = null;
    private int selectedSlotQuantity = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trading_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        Room hubRoom = repo.getCurrentRoom();
        if (player == null || hubRoom == null) return;

        goldDisplay = view.findViewById(R.id.trade_gold);
        goldDisplay.setText("Gold: " + player.getGold());

        selectItemBtn = view.findViewById(R.id.btn_select_item);
        quantityInput = view.findViewById(R.id.input_quantity);
        priceInput = view.findViewById(R.id.input_price);
        Button listBtn = view.findViewById(R.id.btn_list_item);

        RecyclerView tradeList = view.findViewById(R.id.trade_list);
        tradeList.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<TradeListing> listings = hubRoom.getTradeListings();
        adapter = new TradeListingAdapter(listings, repo.getItemRegistry(),
                player.getAccessCode(), this);
        tradeList.setAdapter(adapter);

        selectItemBtn.setOnClickListener(v -> showItemPicker(player, repo));
        listBtn.setOnClickListener(v -> listSelectedItem(player, hubRoom, repo));
    }

    private void showItemPicker(Player player, GameRepository repo) {
        List<InventorySlot> inventory = player.getInventory();
        List<String> names = new ArrayList<>();
        List<Integer> slotIndices = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++) {
            InventorySlot slot = inventory.get(i);
            if (!slot.isEmpty()) {
                ItemDef item = repo.getItemRegistry().getItem(slot.getItemId());
                String name = item != null ? item.getDisplayName() : slot.getItemId();
                names.add(name + " x" + slot.getQuantity());
                slotIndices.add(i);
            }
        }

        if (names.isEmpty()) {
            Toast.makeText(requireContext(), "No items in inventory", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Item to Sell")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    selectedInventorySlot = slotIndices.get(which);
                    InventorySlot slot = inventory.get(selectedInventorySlot);
                    selectedItemId = slot.getItemId();
                    selectedSlotQuantity = slot.getQuantity();
                    ItemDef item = repo.getItemRegistry().getItem(selectedItemId);
                    String displayName = item != null ? item.getDisplayName() : selectedItemId;
                    selectItemBtn.setText(displayName + " x" + selectedSlotQuantity);

                    // Default quantity to full stack
                    quantityInput.setText(String.valueOf(selectedSlotQuantity));

                    // Suggest sell price scaled by quantity
                    if (item != null && item.getSellPrice() > 0) {
                        priceInput.setText(String.valueOf(item.getSellPrice() * selectedSlotQuantity));
                    }
                })
                .show();
    }

    private void listSelectedItem(Player player, Room hubRoom, GameRepository repo) {
        if (selectedInventorySlot < 0 || selectedItemId == null) {
            Toast.makeText(requireContext(), "Select an item first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse quantity
        String qtyText = quantityInput.getText().toString().trim();
        if (qtyText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int listQty;
        try {
            listQty = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listQty <= 0) {
            Toast.makeText(requireContext(), "Quantity must be positive", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse price
        String priceText = priceInput.getText().toString().trim();
        if (priceText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a price", Toast.LENGTH_SHORT).show();
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceText);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid price", Toast.LENGTH_SHORT).show();
            return;
        }

        if (price <= 0) {
            Toast.makeText(requireContext(), "Price must be positive", Toast.LENGTH_SHORT).show();
            return;
        }

        InventorySlot slot = player.getInventory().get(selectedInventorySlot);
        if (slot.isEmpty() || !selectedItemId.equals(slot.getItemId())) {
            Toast.makeText(requireContext(), "Item no longer available", Toast.LENGTH_SHORT).show();
            resetSelection();
            return;
        }

        if (listQty > slot.getQuantity()) {
            Toast.makeText(requireContext(), "Only " + slot.getQuantity() + " available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Deduct listed quantity from inventory slot
        int remaining = slot.getQuantity() - listQty;
        if (remaining <= 0) {
            slot.setItemId(null);
            slot.setQuantity(0);
        } else {
            slot.setQuantity(remaining);
        }

        // Add trade listing to hub room
        TradeListing listing = new TradeListing(selectedItemId, listQty, price,
                player.getName(), player.getAccessCode());
        hubRoom.getTradeListings().add(listing);

        // Persist both player and room
        repo.savePlayer();
        repo.saveCurrentRoom();

        adapter.notifyItemInserted(hubRoom.getTradeListings().size() - 1);
        goldDisplay.setText("Gold: " + player.getGold());

        ItemDef item = repo.getItemRegistry().getItem(selectedItemId);
        String displayName = item != null ? item.getDisplayName() : selectedItemId;
        Toast.makeText(requireContext(), "Listed " + displayName + " x" + listQty + " for " + price + "g",
                Toast.LENGTH_SHORT).show();

        resetSelection();
    }

    private void resetSelection() {
        selectedInventorySlot = -1;
        selectedItemId = null;
        selectedSlotQuantity = 0;
        selectItemBtn.setText("Select Item to Sell");
        quantityInput.setText("");
        priceInput.setText("");
    }

    @Override
    public void onBuy(int position, TradeListing listing) {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        Room hubRoom = repo.getCurrentRoom();
        if (player == null || hubRoom == null) return;

        if (player.getAccessCode().equals(listing.getSellerAccessCode())) {
            Toast.makeText(requireContext(), "You can't buy your own listing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (player.getGold() < listing.getPrice()) {
            Toast.makeText(requireContext(), "Not enough gold!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean added = player.addItemToInventory(listing.getItemId(), listing.getQuantity());
        if (!added) {
            Toast.makeText(requireContext(), "Inventory full!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deduct gold from buyer
        player.setGold(player.getGold() - listing.getPrice());

        // Remove the listing
        hubRoom.getTradeListings().remove(position);

        // Persist
        repo.savePlayer();
        repo.saveCurrentRoom();

        adapter.notifyItemRemoved(position);
        goldDisplay.setText("Gold: " + player.getGold());

        ItemDef item = repo.getItemRegistry().getItem(listing.getItemId());
        String displayName = item != null ? item.getDisplayName() : listing.getItemId();
        Toast.makeText(requireContext(), "Bought " + displayName + " from " + listing.getSellerName(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancel(int position, TradeListing listing) {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        Room hubRoom = repo.getCurrentRoom();
        if (player == null || hubRoom == null) return;

        // Return item to seller's inventory
        boolean added = player.addItemToInventory(listing.getItemId(), listing.getQuantity());
        if (!added) {
            Toast.makeText(requireContext(), "Inventory full! Can't cancel listing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove listing
        hubRoom.getTradeListings().remove(position);

        // Persist
        repo.savePlayer();
        repo.saveCurrentRoom();

        adapter.notifyItemRemoved(position);

        ItemDef item = repo.getItemRegistry().getItem(listing.getItemId());
        String displayName = item != null ? item.getDisplayName() : listing.getItemId();
        Toast.makeText(requireContext(), "Cancelled listing for " + displayName,
                Toast.LENGTH_SHORT).show();
    }
}
