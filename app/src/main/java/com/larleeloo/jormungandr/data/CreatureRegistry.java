package com.larleeloo.jormungandr.data;

import android.content.Context;

import com.larleeloo.jormungandr.model.CreatureDef;
import com.larleeloo.jormungandr.util.SeededRandom;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureRegistry {
    private final Map<String, CreatureDef> creaturesById = new HashMap<>();
    private final Map<Integer, List<CreatureDef>> creaturesByRegion = new HashMap<>();
    private boolean loaded = false;

    public void load(Context context) {
        if (loaded) return;
        try {
            InputStream is = context.getAssets().open("data/creatures.json");
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            is.close();
            String json = new String(bytes, StandardCharsets.UTF_8);
            List<CreatureDef> creatures = JsonHelper.listFromJson(json, CreatureDef.class);

            for (CreatureDef creature : creatures) {
                creaturesById.put(creature.getCreatureId(), creature);
                int region = creature.getRegion();
                if (!creaturesByRegion.containsKey(region)) {
                    creaturesByRegion.put(region, new ArrayList<>());
                }
                creaturesByRegion.get(region).add(creature);
            }
            loaded = true;
        } catch (IOException e) {
            android.util.Log.w("CreatureRegistry", "Failed to load creatures", e);
        }
    }

    public CreatureDef getCreature(String creatureId) {
        return creaturesById.get(creatureId);
    }

    public List<CreatureDef> getCreaturesForRegion(int region) {
        List<CreatureDef> creatures = creaturesByRegion.get(region);
        return creatures != null ? creatures : new ArrayList<>();
    }

    public CreatureDef getRandomCreatureForRegion(SeededRandom rng, int region) {
        List<CreatureDef> creatures = getCreaturesForRegion(region);
        if (creatures.isEmpty()) {
            // Fallback to region 1
            creatures = getCreaturesForRegion(1);
        }
        if (creatures.isEmpty()) return null;
        return creatures.get(rng.nextInt(creatures.size()));
    }

    public boolean isLoaded() { return loaded; }
}
