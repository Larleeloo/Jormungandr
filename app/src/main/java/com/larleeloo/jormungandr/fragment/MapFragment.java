package com.larleeloo.jormungandr.fragment;

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
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.BiomeType;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.Constants;
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.view.GridMapCanvasView;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private GridMapCanvasView gridMapCanvas;
    private final List<Button> tabButtons = new ArrayList<>();
    private int selectedRegion = -1;

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

        String roomId = player.getCurrentRoomId();
        int currentRegion = player.getCurrentRegion();
        BiomeType biome = BiomeType.fromRegion(currentRegion);

        mapLocation.setText(biome.getDisplayName() + " - " + roomId);
        gridMapCanvas.setPlayer(player, roomId);

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
}
