package com.example.jormungandr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jormungandr.R;
import com.example.jormungandr.adapter.InventoryAdapter;
import com.example.jormungandr.data.GameRepository;
import com.example.jormungandr.model.InventorySlot;
import com.example.jormungandr.model.Player;
import com.example.jormungandr.util.Constants;

import java.util.ArrayList;

public class TransferFragment extends Fragment {

    private InventoryAdapter playerAdapter;
    private InventoryAdapter storageAdapter;
    private int selectedPlayerSlot = -1;
    private int selectedStorageSlot = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView playerList = view.findViewById(R.id.player_inventory_list);
        RecyclerView storageList = view.findViewById(R.id.storage_list);

        playerList.setLayoutManager(new LinearLayoutManager(requireContext()));
        storageList.setLayoutManager(new LinearLayoutManager(requireContext()));

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        // Ensure bank storage has slots
        while (player.getBankStorage().size() < Constants.MAX_INVENTORY_SLOTS) {
            player.getBankStorage().add(new InventorySlot(null, 0, player.getBankStorage().size()));
        }

        playerAdapter = new InventoryAdapter(player.getInventory(), repo.getItemRegistry(),
                (pos, slot) -> { selectedPlayerSlot = pos; selectedStorageSlot = -1; });
        storageAdapter = new InventoryAdapter(player.getBankStorage(), repo.getItemRegistry(),
                (pos, slot) -> { selectedStorageSlot = pos; selectedPlayerSlot = -1; });

        playerList.setAdapter(playerAdapter);
        storageList.setAdapter(storageAdapter);

        view.findViewById(R.id.btn_to_storage).setOnClickListener(v -> transferToStorage(player, repo));
        view.findViewById(R.id.btn_to_inventory).setOnClickListener(v -> transferToInventory(player, repo));
    }

    private void transferToStorage(Player player, GameRepository repo) {
        if (selectedPlayerSlot < 0) {
            Toast.makeText(requireContext(), "Select an inventory item first", Toast.LENGTH_SHORT).show();
            return;
        }

        InventorySlot source = player.getInventory().get(selectedPlayerSlot);
        if (source.isEmpty()) return;

        // Find empty storage slot
        for (InventorySlot dest : player.getBankStorage()) {
            if (dest.isEmpty()) {
                dest.setItemId(source.getItemId());
                dest.setQuantity(source.getQuantity());
                source.setItemId(null);
                source.setQuantity(0);

                repo.savePlayer();
                playerAdapter.notifyDataSetChanged();
                storageAdapter.notifyDataSetChanged();
                selectedPlayerSlot = -1;
                return;
            }
        }
        Toast.makeText(requireContext(), "Storage full!", Toast.LENGTH_SHORT).show();
    }

    private void transferToInventory(Player player, GameRepository repo) {
        if (selectedStorageSlot < 0) {
            Toast.makeText(requireContext(), "Select a storage item first", Toast.LENGTH_SHORT).show();
            return;
        }

        InventorySlot source = player.getBankStorage().get(selectedStorageSlot);
        if (source.isEmpty()) return;

        boolean added = player.addItemToInventory(source.getItemId(), source.getQuantity());
        if (added) {
            source.setItemId(null);
            source.setQuantity(0);
            repo.savePlayer();
            playerAdapter.notifyDataSetChanged();
            storageAdapter.notifyDataSetChanged();
            selectedStorageSlot = -1;
        } else {
            Toast.makeText(requireContext(), "Inventory full!", Toast.LENGTH_SHORT).show();
        }
    }
}
