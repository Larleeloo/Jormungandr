package com.larleeloo.jormungandr.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.cloud.CloudSyncManager;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.fragment.CharacterFragment;
import com.larleeloo.jormungandr.fragment.CombatFragment;
import com.larleeloo.jormungandr.fragment.InventoryFragment;
import com.larleeloo.jormungandr.fragment.LoadingFragment;
import com.larleeloo.jormungandr.fragment.MapFragment;
import com.larleeloo.jormungandr.fragment.RoomFragment;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.Constants;

public class GameActivity extends AppCompatActivity {

    /** Minimum time (ms) to display the loading screen between rooms. */
    private static final long LOADING_SCREEN_MIN_MS = 1200;

    private TextView hudHp, hudMana, hudStamina, hudLevel, syncIndicator;
    private Button btnInventory, btnCharacter, btnMap, btnRoom;
    private Fragment currentFragment;
    private String currentFragmentTag;
    private final Handler syncUiHandler = new Handler(Looper.getMainLooper());
    private Runnable syncHideRunnable;

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

        // Sync status indicator
        syncIndicator = findViewById(R.id.sync_indicator);

        if (Constants.APPS_SCRIPT_URL.isEmpty()) {
            showSyncStatus(false, "Cloud sync not configured. Set APPS_SCRIPT_URL in Constants.java");
        }

        // Load the initial room asynchronously so the cloud fetch runs off
        // the main thread (where network I/O is forbidden on Android).
        GameRepository repo = GameRepository.getInstance(this);
        Player player = repo.getCurrentPlayer();
        String startRoom = Constants.HUB_ROOM_ID;
        if (player != null) {
            startRoom = player.getCurrentRoomId();
        }

        showFragment(new LoadingFragment(), "loading");
        showSyncStatus(true, "Syncing...");
        repo.loadOrGenerateRoomAsync(startRoom, room -> {
            updateHud();
            showSyncStatus(true, "Synced");
            showFragment(new RoomFragment(), "room");
        });
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

        // Upload the room we're LEAVING to cloud (fire-and-forget async)
        repo.saveCurrentRoom();

        // If the room is cached, skip the loading screen entirely
        if (repo.getCachedRoom(roomId) != null) {
            repo.navigateToRoomAsync(roomId, room -> {
                updateHud();
                showFragment(new RoomFragment(), "room");
            });
            return;
        }

        // No cache hit — show loading screen while fetching from cloud
        showFragment(new LoadingFragment(), "loading");

        long loadStart = System.currentTimeMillis();
        showSyncStatus(true, "Syncing...");

        repo.navigateToRoomAsync(roomId, room -> {
            updateHud();
            showSyncStatus(true, "Synced");

            // Ensure the loading screen shows for at least LOADING_SCREEN_MIN_MS
            long elapsed = System.currentTimeMillis() - loadStart;
            long remaining = Math.max(0, LOADING_SCREEN_MIN_MS - elapsed);

            syncUiHandler.postDelayed(() -> showFragment(new RoomFragment(), "room"), remaining);
        });
    }

    public void startCombat(String creatureDefId, int level, int hp) {
        CombatFragment combat = CombatFragment.newInstance(creatureDefId, level, hp);
        showFragment(combat, "combat");
    }

    public void returnFromCombat(boolean victory) {
        updateHud();
        GameRepository repo = GameRepository.getInstance(this);
        Player player = repo.getCurrentPlayer();
        CloudSyncManager cloudSync = repo.getCloudSyncManager();

        if (victory) {
            // Sync player and room after combat victory
            if (player != null) {
                showSyncStatus(true, "Syncing...");
                cloudSync.syncPlayerToCloud(player, (success, message) ->
                        showSyncStatus(success, message));
                repo.saveCurrentRoom();
            }
            showFragment(new RoomFragment(), "room");
        } else {
            // Death: return to hub
            if (player != null) {
                // Upload the room where player died
                repo.saveCurrentRoom();

                player.setHp(player.getMaxHp() / 2);

                // Navigate to hub asynchronously so cloud fetch works
                showFragment(new LoadingFragment(), "loading");
                showSyncStatus(true, "Syncing...");
                repo.navigateToRoomAsync(Constants.HUB_ROOM_ID, room -> {
                    updateHud();
                    showSyncStatus(true, "Synced");
                    showFragment(new RoomFragment(), "room");
                });
            } else {
                showFragment(new RoomFragment(), "room");
            }
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

    private void showSyncStatus(boolean success, String message) {
        if (syncIndicator == null) return;

        // Cancel any pending hide
        if (syncHideRunnable != null) {
            syncUiHandler.removeCallbacks(syncHideRunnable);
        }

        syncIndicator.setVisibility(android.view.View.VISIBLE);
        syncIndicator.setText(message);

        if (message.contains("Syncing")) {
            syncIndicator.setTextColor(0xFFAAAAAA); // gray while in progress
        } else if (success) {
            syncIndicator.setTextColor(0xFF44FF44); // green on success
            // Auto-hide success after 3 seconds
            syncHideRunnable = () -> syncIndicator.setVisibility(android.view.View.GONE);
            syncUiHandler.postDelayed(syncHideRunnable, 3000);
        } else {
            syncIndicator.setTextColor(0xFFFF4444); // red on failure
            // Keep errors visible longer (8 seconds)
            syncHideRunnable = () -> syncIndicator.setVisibility(android.view.View.GONE);
            syncUiHandler.postDelayed(syncHideRunnable, 8000);
        }
    }

    private int resolveColor(int resId) {
        return getResources().getColor(resId, getTheme());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Auto-save on pause via repo's CloudSyncManager (async)
        GameRepository repo = GameRepository.getInstance(this);
        repo.savePlayer();
        repo.saveCurrentRoom();
    }

    @Override
    public void onBackPressed() {
        if ("hub".equals(currentFragmentTag)) {
            // Return from HubFragment portal view back to the room canvas view
            showFragment(new RoomFragment(), "room");
        } else if (!"room".equals(currentFragmentTag)) {
            // Return to the current room canvas view from shop/inventory/etc.
            showFragment(new RoomFragment(), "room");
        } else {
            super.onBackPressed();
        }
    }
}
