package com.larleeloo.jormungandr.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.activity.GameActivity;
import com.larleeloo.jormungandr.cloud.AccessCodeValidator;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.engine.WorldMesh;
import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Direction;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.Room;
import com.larleeloo.jormungandr.model.RoomObject;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.view.GridMapCanvasView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment {

    private GridMapCanvasView gridMapCanvas;
    private final List<Button> tabButtons = new ArrayList<>();
    private int selectedRegion = -1;
    private boolean isAdmin = false;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridMapCanvas = view.findViewById(R.id.grid_map_canvas);
        TextView mapLocation = view.findViewById(R.id.map_location);
        LinearLayout regionTabs = view.findViewById(R.id.region_tabs);
        LinearLayout waypointPanel = view.findViewById(R.id.waypoint_panel);
        Button btnTeleportHub = view.findViewById(R.id.btn_teleport_hub);

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();

        if (player == null) return;

        isAdmin = AccessCodeValidator.isAdminCode(player.getAccessCode());

        String roomId = player.getCurrentRoomId();
        int currentRegion = player.getCurrentRegion();
        BiomeType biome = BiomeType.fromRegion(currentRegion);

        String locationText = biome.getDisplayName() + " - " + roomId;
        if (isAdmin) locationText = "[ADMIN] " + locationText;
        mapLocation.setText(locationText);

        gridMapCanvas.setPlayer(player, roomId);

        if (isAdmin) {
            gridMapCanvas.setAdminMode(true);
            gridMapCanvas.setOnRoomTapListener(this::showRoomInspector);
        }

        // Build region tabs: Hub + regions 1-8
        buildRegionTabs(regionTabs, currentRegion);

        // Select the player's current region tab
        selectRegion(currentRegion);

        // Waypoint panel
        if (!player.getDiscoveredWaypoints().isEmpty()) {
            waypointPanel.setVisibility(View.VISIBLE);
        }

        btnTeleportHub.setOnClickListener(v -> {
            GameActivity activity = (GameActivity) getActivity();
            if (activity != null) {
                activity.navigateToRoom(Constants.HUB_ROOM_ID);
            }
        });
    }

    /**
     * Show a dialog inspecting a tapped room (admin only).
     * Loads room data from cloud off the main thread, then displays room info,
     * contents summary, and a "Travel Here" button.
     */
    private void showRoomInspector(String roomId, int region, int row, int col) {
        if (!isAdded()) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        WorldMesh mesh = WorldMesh.getInstance();
        BiomeType biome = BiomeType.fromRegion(region);

        // Build the mesh info (local, no network)
        StringBuilder info = new StringBuilder();
        info.append("Room: ").append(roomId).append("\n");
        info.append("Region: ").append(region).append(" (").append(biome.getDisplayName()).append(")\n");
        info.append("Grid: row ").append(row).append(", col ").append(col).append("\n");

        boolean isWaypoint = RoomIdHelper.isWaypoint(region, RoomIdHelper.toRoomNumber(row, col));
        if (isWaypoint) info.append("** WAYPOINT **\n");

        // Show doors from mesh
        Map<Direction, String> doors = mesh.getNeighbors(roomId);
        info.append("\nDoors (").append(doors.size()).append("):\n");
        for (Map.Entry<Direction, String> entry : doors.entrySet()) {
            String target = entry.getValue();
            int targetRegion = RoomIdHelper.getRegion(target);
            if (targetRegion != region) {
                info.append("  ").append(entry.getKey().getDisplayName())
                        .append(" -> PORTAL to R").append(targetRegion).append("\n");
            } else {
                info.append("  ").append(entry.getKey().getDisplayName())
                        .append(" -> ").append(target).append("\n");
            }
        }

        String meshInfo = info.toString();

        // Load room contents from cloud off main thread
        ioExecutor.execute(() -> {
            Room room = repo.getRoomFileManager().loadRoom(roomId);
            StringBuilder contentInfo = new StringBuilder(meshInfo);

            if (room != null) {
                contentInfo.append("\nContents (visited):\n");
                int chests = 0, chestsClosed = 0;
                int creatures = 0, creaturesAlive = 0;
                int traps = 0, trapsTriggered = 0;
                int items = 0;

                for (RoomObject obj : room.getObjects()) {
                    switch (obj.getType()) {
                        case "chest":
                            chests++;
                            if (!obj.isOpened()) chestsClosed++;
                            break;
                        case "creature":
                            creatures++;
                            if (obj.isAlive()) creaturesAlive++;
                            break;
                        case "trap":
                            traps++;
                            if (obj.isTriggered()) trapsTriggered++;
                            break;
                        case "item":
                            if (obj.getQuantity() > 0) items++;
                            break;
                    }
                }

                if (chests > 0) contentInfo.append("  Chests: ").append(chestsClosed).append("/").append(chests).append(" unopened\n");
                if (creatures > 0) contentInfo.append("  Creatures: ").append(creaturesAlive).append("/").append(creatures).append(" alive\n");
                if (traps > 0) contentInfo.append("  Traps: ").append(trapsTriggered).append("/").append(traps).append(" triggered\n");
                if (items > 0) contentInfo.append("  Floor items: ").append(items).append("\n");
                if (room.getFirstVisitedBy() != null) {
                    contentInfo.append("  First visitor: ").append(room.getFirstVisitedBy()).append("\n");
                }
            } else {
                contentInfo.append("\nNot yet visited (room will be generated on travel).\n");
            }

            String finalInfo = contentInfo.toString();

            // Post dialog back to main thread
            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Room Inspector")
                            .setMessage(finalInfo)
                            .setPositiveButton("Travel Here", (dialog, which) -> {
                                GameActivity activity = (GameActivity) getActivity();
                                if (activity != null) {
                                    activity.navigateToRoom(roomId);
                                }
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
            }
        });
    }

    private void buildRegionTabs(LinearLayout container, int currentRegion) {
        tabButtons.clear();

        // Hub tab
        addTab(container, "Hub", BiomeType.HUB.getColor(), 0);

        // Region tabs 1-8
        for (int r = 1; r <= Constants.NUM_REGIONS; r++) {
            BiomeType biome = BiomeType.fromRegion(r);
            String label = "R" + r;
            addTab(container, label, biome.getColor(), r);
        }
    }

    private void addTab(LinearLayout container, String label, int color, int region) {
        Button btn = new Button(requireContext());
        btn.setText(label);
        btn.setTextSize(11f);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(32));
        lp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
        btn.setLayoutParams(lp);
        btn.setPadding(dpToPx(10), 0, dpToPx(10), 0);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(4));
        bg.setColor(0xFF222244);
        btn.setBackground(bg);

        btn.setOnClickListener(v -> selectRegion(region));

        container.addView(btn);
        tabButtons.add(btn);
    }

    private void selectRegion(int region) {
        selectedRegion = region;

        // Update tab styles
        for (int i = 0; i < tabButtons.size(); i++) {
            Button btn = tabButtons.get(i);
            int tabRegion = i; // 0=hub, 1-8=regions
            BiomeType biome = BiomeType.fromRegion(tabRegion);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(4));

            if (tabRegion == region) {
                bg.setColor(biome.getColor());
                bg.setAlpha(200);
                btn.setTextColor(Color.WHITE);
            } else {
                bg.setColor(0xFF222244);
                btn.setTextColor(0xFFAAAAAA);
            }
            btn.setBackground(bg);
        }

        gridMapCanvas.setDisplayRegion(region);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}
