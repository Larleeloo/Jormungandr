package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.activity.GameActivity;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.engine.PlayerLevelManager;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.PlayerStats;

public class CharacterFragment extends Fragment {

    private TextView playerName, playerLevel, statHp, statMana, statStamina, availablePoints, xpText;
    private ProgressBar xpBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_character, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerName = view.findViewById(R.id.player_name);
        playerLevel = view.findViewById(R.id.player_level);
        statHp = view.findViewById(R.id.stat_hp);
        statMana = view.findViewById(R.id.stat_mana);
        statStamina = view.findViewById(R.id.stat_stamina);
        availablePoints = view.findViewById(R.id.available_points);
        xpBar = view.findViewById(R.id.xp_bar);
        xpText = view.findViewById(R.id.xp_text);

        setupStatRow(view.findViewById(R.id.stat_strength), "Strength", "strength",
                "+2 inventory slots per point. Increases melee damage.");
        setupStatRow(view.findViewById(R.id.stat_constitution), "Constitution", "constitution",
                "Increases max HP. Improves resistance to poison and traps.");
        setupStatRow(view.findViewById(R.id.stat_intelligence), "Intelligence", "intelligence",
                "Increases max Mana. Boosts spell and scroll effectiveness.");
        setupStatRow(view.findViewById(R.id.stat_wisdom), "Wisdom", "wisdom",
                "8% trap detection per point. Reveals hidden objects.");
        setupStatRow(view.findViewById(R.id.stat_charisma), "Charisma", "charisma",
                "Better shop prices. Improves note visibility to others.");
        setupStatRow(view.findViewById(R.id.stat_dexterity), "Dexterity", "dexterity",
                "Increases dodge chance. Boosts ranged damage and flee success.");

        refreshDisplay();
    }

    private void setupStatRow(View row, String label, String statKey, String description) {
        if (row == null) return;
        TextView labelView = row.findViewById(R.id.stat_label);
        TextView valueView = row.findViewById(R.id.stat_value);
        TextView descView = row.findViewById(R.id.stat_desc);
        Button plusBtn = row.findViewById(R.id.stat_plus);

        if (labelView != null) labelView.setText(label);
        if (descView != null) {
            descView.setText(description);
            descView.setVisibility(View.VISIBLE);
        }

        plusBtn.setOnClickListener(v -> {
            GameRepository repo = GameRepository.getInstance(requireContext());
            Player player = repo.getCurrentPlayer();
            if (player == null) return;

            if (PlayerLevelManager.investStatPoint(player, statKey)) {
                repo.savePlayer();
                refreshDisplay();
                GameActivity activity = (GameActivity) getActivity();
                if (activity != null) activity.updateHud();
            } else {
                Toast.makeText(requireContext(), "No stat points available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshDisplay() {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        PlayerStats stats = player.getStats();

        playerName.setText(player.getName());
        playerLevel.setText("Level " + player.getLevel());
        statHp.setText("HP: " + player.getHp() + "/" + player.getMaxHp());
        statMana.setText("Mana: " + player.getMana() + "/" + player.getMaxMana());
        statStamina.setText("Stamina: " + player.getStamina() + "/" + player.getMaxStamina());
        availablePoints.setText("Available Points: " + stats.getAvailablePoints());

        xpBar.setMax(player.getXpToNext());
        xpBar.setProgress(player.getXp());
        xpText.setText(player.getXp() + "/" + player.getXpToNext());

        View view = getView();
        if (view == null) return;

        updateStatValue(view.findViewById(R.id.stat_strength), stats.getStrength());
        updateStatValue(view.findViewById(R.id.stat_constitution), stats.getConstitution());
        updateStatValue(view.findViewById(R.id.stat_intelligence), stats.getIntelligence());
        updateStatValue(view.findViewById(R.id.stat_wisdom), stats.getWisdom());
        updateStatValue(view.findViewById(R.id.stat_charisma), stats.getCharisma());
        updateStatValue(view.findViewById(R.id.stat_dexterity), stats.getDexterity());
    }

    private void updateStatValue(View row, int value) {
        if (row == null) return;
        TextView valueView = row.findViewById(R.id.stat_value);
        if (valueView != null) valueView.setText(String.valueOf(value));
    }
}
