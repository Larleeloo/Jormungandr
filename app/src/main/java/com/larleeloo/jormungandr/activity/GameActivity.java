package com.larleeloo.jormungandr.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.fragment.CharacterFragment;
import com.larleeloo.jormungandr.fragment.CombatFragment;
import com.larleeloo.jormungandr.fragment.HubFragment;
import com.larleeloo.jormungandr.fragment.InventoryFragment;
import com.larleeloo.jormungandr.fragment.MapFragment;
import com.larleeloo.jormungandr.fragment.RoomFragment;
import com.larleeloo.jormungandr.model.Player;

public class GameActivity extends AppCompatActivity {

    private TextView hudHp, hudMana, hudStamina, hudLevel;
    private Button btnInventory, btnCharacter, btnMap, btnRoom;
    private Fragment currentFragment;
    private String currentFragmentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.game_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // HUD elements
        hudHp = findViewById(R.id.hud_hp);
        hudMana = findViewById(R.id.hud_mana);
        hudStamina = findViewById(R.id.hud_stamina);
        hudLevel = findViewById(R.id.hud_level);

        // Bottom bar buttons
        btnInventory = findViewById(R.id.btn_inventory);
        btnCharacter = findViewById(R.id.btn_character);
        btnMap = findViewById(R.id.btn_map);
        btnRoom = findViewById(R.id.btn_room);

        btnInventory.setOnClickListener(v -> showFragment(new InventoryFragment(), "inventory"));
        btnCharacter.setOnClickListener(v -> showFragment(new CharacterFragment(), "character"));
        btnMap.setOnClickListener(v -> showFragment(new MapFragment(), "map"));
        btnRoom.setOnClickListener(v -> showFragment(new RoomFragment(), "room"));

        // Load the initial room
        GameRepository repo = GameRepository.getInstance(this);
        Player player = repo.getCurrentPlayer();
        if (player != null) {
            String startRoom = player.getCurrentRoomId();
            repo.loadOrGenerateRoom(startRoom);
        }

        updateHud();
        showFragment(new RoomFragment(), "room");
    }

    public void showFragment(Fragment fragment, String tag) {
        currentFragment = fragment;
        currentFragmentTag = tag;

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment, tag);
        tx.commit();

        updateBottomBar(tag);
    }

    public void navigateToRoom(String roomId) {
        GameRepository repo = GameRepository.getInstance(this);
        repo.navigateToRoom(roomId);
        updateHud();
        showFragment(new RoomFragment(), "room");
    }

    public void startCombat(String creatureDefId, int level, int hp) {
        CombatFragment combat = CombatFragment.newInstance(creatureDefId, level, hp);
        showFragment(combat, "combat");
    }

    public void returnFromCombat(boolean victory) {
        updateHud();
        if (victory) {
            showFragment(new RoomFragment(), "room");
        } else {
            // Death: return to hub
            GameRepository repo = GameRepository.getInstance(this);
            Player player = repo.getCurrentPlayer();
            if (player != null) {
                player.setHp(player.getMaxHp() / 2);
                player.setRoomsVisitedSinceHub(0);
                repo.navigateToRoom("r0_00000");
                repo.savePlayer();
            }
            showFragment(new RoomFragment(), "room");
        }
    }

    public void updateHud() {
        GameRepository repo = GameRepository.getInstance(this);
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        hudHp.setText("HP:" + player.getHp() + "/" + player.getMaxHp());
        hudMana.setText("MP:" + player.getMana() + "/" + player.getMaxMana());
        hudStamina.setText("SP:" + player.getStamina() + "/" + player.getMaxStamina());
        hudLevel.setText("Lv." + player.getLevel());
    }

    private void updateBottomBar(String activeTag) {
        btnInventory.setTextColor(resolveColor("inventory".equals(activeTag) ? R.color.gold : R.color.white));
        btnCharacter.setTextColor(resolveColor("character".equals(activeTag) ? R.color.gold : R.color.white));
        btnMap.setTextColor(resolveColor("map".equals(activeTag) ? R.color.gold : R.color.white));
        btnRoom.setTextColor(resolveColor("room".equals(activeTag) ? R.color.gold : R.color.white));
    }

    private int resolveColor(int resId) {
        return getResources().getColor(resId, getTheme());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Auto-save on pause
        GameRepository repo = GameRepository.getInstance(this);
        repo.savePlayer();
        repo.saveCurrentRoom();
    }

    @Override
    public void onBackPressed() {
        if (!"room".equals(currentFragmentTag)) {
            showFragment(new RoomFragment(), "room");
        } else {
            super.onBackPressed();
        }
    }
}
