package com.example.jormungandr.data;

import android.content.Context;

import com.example.jormungandr.model.ItemDef;
import com.example.jormungandr.model.Rarity;
import com.example.jormungandr.util.SeededRandom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRegistry {
    private final Map<String, ItemDef> itemsById = new HashMap<>();
    private final Map<Rarity, List<ItemDef>> itemsByRarity = new HashMap<>();
    private boolean loaded = false;

    public void load(Context context) {
        if (loaded) return;
        try {
            InputStream is = context.getAssets().open("data/items.json");
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            is.close();
            String json = new String(bytes, StandardCharsets.UTF_8);
            List<ItemDef> items = JsonHelper.listFromJson(json, ItemDef.class);

            for (ItemDef item : items) {
                itemsById.put(item.getItemId(), item);
                Rarity rarity = item.getRarityEnum();
                if (!itemsByRarity.containsKey(rarity)) {
                    itemsByRarity.put(rarity, new ArrayList<>());
                }
                itemsByRarity.get(rarity).add(item);
            }
            loaded = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ItemDef getItem(String itemId) {
        return itemsById.get(itemId);
    }

    public List<ItemDef> getItemsByRarity(Rarity rarity) {
        List<ItemDef> items = itemsByRarity.get(rarity);
        return items != null ? items : new ArrayList<>();
    }

    public ItemDef getRandomItemByRarity(SeededRandom rng, Rarity rarity) {
        List<ItemDef> items = getItemsByRarity(rarity);
        if (items.isEmpty()) {
            // Fallback to common
            items = getItemsByRarity(Rarity.COMMON);
        }
        if (items.isEmpty()) return null;
        return items.get(rng.nextInt(items.size()));
    }

    public List<ItemDef> getAllItems() {
        return new ArrayList<>(itemsById.values());
    }

    public boolean isLoaded() { return loaded; }
}
