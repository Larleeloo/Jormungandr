package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.adapter.ShopAdapter;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.Player;

import java.util.ArrayList;
import java.util.List;

public class ShopFragment extends Fragment implements ShopAdapter.OnBuyClickListener {

    private TextView goldDisplay;
    private ShopAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        goldDisplay = view.findViewById(R.id.shop_gold);
        RecyclerView shopList = view.findViewById(R.id.shop_list);
        shopList.setLayoutManager(new LinearLayoutManager(requireContext()));

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();

        if (player != null) {
            goldDisplay.setText("Gold: " + player.getGold());
        }

        // Build shop inventory from common/uncommon items
        List<ItemDef> shopItems = new ArrayList<>();
        for (ItemDef item : repo.getItemRegistry().getAllItems()) {
            if (item.getBuyPrice() > 0) {
                shopItems.add(item);
            }
        }

        // Sort by price
        shopItems.sort((a, b) -> Integer.compare(a.getBuyPrice(), b.getBuyPrice()));

        // Limit to first 30 items
        if (shopItems.size() > 30) {
            shopItems = shopItems.subList(0, 30);
        }

        adapter = new ShopAdapter(shopItems, this);
        shopList.setAdapter(adapter);
    }

    @Override
    public void onBuy(ItemDef item) {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        if (player.getGold() < item.getBuyPrice()) {
            Toast.makeText(requireContext(), "Not enough gold!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean added = player.addItemToInventory(item.getItemId(), 1);
        if (!added) {
            Toast.makeText(requireContext(), "Inventory full!", Toast.LENGTH_SHORT).show();
            return;
        }

        player.setGold(player.getGold() - item.getBuyPrice());
        repo.savePlayer();
        goldDisplay.setText("Gold: " + player.getGold());
        Toast.makeText(requireContext(), "Bought " + item.getDisplayName(), Toast.LENGTH_SHORT).show();
    }
}
