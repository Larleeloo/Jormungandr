package com.larleeloo.jormungandr.fragment;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.data.CreatureRegistry;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.model.CreatureDef;
import com.larleeloo.jormungandr.model.ItemDef;

import java.util.List;
import java.util.Random;

/**
 * Fun loading screen shown between room transitions.
 * Displays a random item or creature with a thematic tip or flavor text.
 */
public class LoadingFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int dotState;
    private Runnable dotAnimator;

    private static final String[] TRAVEL_VERBS = {
            "Traversing", "Exploring", "Venturing into", "Wandering toward",
            "Sneaking into", "Stumbling upon", "Marching to", "Descending into"
    };

    private static final String[] ITEM_TIPS = {
            "Torches reveal hidden treasures in dark rooms.",
            "Stack food to restore health between fights.",
            "Rare items glow with an otherworldly shimmer.",
            "Keys unlock doors to chambers long sealed.",
            "Potions can turn the tide of any battle.",
            "Every chest might hold something legendary.",
            "Scrolls contain ancient magic from forgotten ages.",
            "The best armor is forged in the deepest regions.",
            "Some materials can be traded at waypoints.",
            "Leather is prized by crafters at waypoint shops."
    };

    private static final String[] CREATURE_TIPS = {
            "Creatures grow stronger deeper into each region.",
            "Some creatures guard rare loot worth the fight.",
            "Defeating a creature clears the path forward.",
            "Watch your stamina in extended fights.",
            "Wisdom helps detect traps near creature dens.",
            "Each region breeds unique and dangerous beasts.",
            "The deeper you go, the greater the challenge.",
            "Legendary creatures haunt the ends of deep branches.",
            "Cross-region doors may lead to unexpected dangers.",
            "Waypoints offer safety between dangerous encounters."
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_loading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View sprite = view.findViewById(R.id.loading_sprite);
        TextView nameText = view.findViewById(R.id.loading_name);
        TextView descText = view.findViewById(R.id.loading_description);
        TextView dotsText = view.findViewById(R.id.loading_dots);

        Random rng = new Random();

        // Pick a random travel verb
        String verb = TRAVEL_VERBS[rng.nextInt(TRAVEL_VERBS.length)];

        // 50/50 show an item or a creature
        GameRepository repo = GameRepository.getInstance();
        boolean showItem = rng.nextBoolean();

        if (showItem && repo != null) {
            showRandomItem(repo, rng, sprite, nameText, descText);
        } else if (repo != null) {
            showRandomCreature(repo, rng, sprite, nameText, descText);
        } else {
            nameText.setText("Preparing...");
            descText.setText("The world serpent stirs.");
            setSpriteColor(sprite, 0xFF4B0082);
        }

        // Dot animation
        dotsText.setText(verb + " . . .");
        dotState = 0;
        dotAnimator = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                dotState = (dotState + 1) % 4;
                StringBuilder dots = new StringBuilder(verb + " ");
                for (int i = 0; i < dotState + 1; i++) dots.append(". ");
                dotsText.setText(dots.toString().trim());
                handler.postDelayed(this, 400);
            }
        };
        handler.postDelayed(dotAnimator, 400);
    }

    private void showRandomItem(GameRepository repo, Random rng,
                                View sprite, TextView nameText, TextView descText) {
        ItemRegistry itemRegistry = repo.getItemRegistry();
        List<ItemDef> allItems = itemRegistry.getAllItems();
        if (allItems == null || allItems.isEmpty()) {
            nameText.setText("Mysterious Object");
            descText.setText(ITEM_TIPS[rng.nextInt(ITEM_TIPS.length)]);
            setSpriteColor(sprite, 0xFFFFD700);
            return;
        }

        ItemDef item = allItems.get(rng.nextInt(allItems.size()));
        nameText.setText(item.getDisplayName());

        // Show either the item description or a random tip
        String desc = item.getDescription();
        if (desc != null && !desc.isEmpty() && rng.nextBoolean()) {
            descText.setText(desc);
        } else {
            descText.setText(ITEM_TIPS[rng.nextInt(ITEM_TIPS.length)]);
        }

        int color = item.getPlaceholderColorInt();
        setSpriteColor(sprite, color != 0 ? color : 0xFFFFD700);
    }

    private void showRandomCreature(GameRepository repo, Random rng,
                                    View sprite, TextView nameText, TextView descText) {
        CreatureRegistry creatureRegistry = repo.getCreatureRegistry();
        // Pick from a random region (1-8)
        int region = rng.nextInt(8) + 1;
        List<CreatureDef> creatures = creatureRegistry.getCreaturesForRegion(region);
        if (creatures == null || creatures.isEmpty()) {
            nameText.setText("Unknown Beast");
            descText.setText(CREATURE_TIPS[rng.nextInt(CREATURE_TIPS.length)]);
            setSpriteColor(sprite, 0xFF8B0000);
            return;
        }

        CreatureDef creature = creatures.get(rng.nextInt(creatures.size()));
        nameText.setText(creature.getDisplayName());

        String desc = creature.getDescription();
        if (desc != null && !desc.isEmpty() && rng.nextBoolean()) {
            descText.setText(desc);
        } else {
            descText.setText(CREATURE_TIPS[rng.nextInt(CREATURE_TIPS.length)]);
        }

        int color = creature.getPlaceholderColorInt();
        setSpriteColor(sprite, color != 0 ? color : 0xFF8B0000);
    }

    private void setSpriteColor(View sprite, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(color);
        bg.setCornerRadius(12f);
        sprite.setBackground(bg);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
