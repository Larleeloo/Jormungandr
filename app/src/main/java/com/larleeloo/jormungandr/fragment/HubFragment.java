package com.larleeloo.jormungandr.fragment;

import android.graphics.Color;
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

import java.util.List;

/**
 * Hub room UI. Shows a dynamic set of portal doors:
 * - Region 1 entrance is always available
 * - Discovered waypoints in other regions appear as portal doors
 * - Shop, storage, and trading post services
 */
public class HubFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GameActivity activity = (GameActivity) getActivity();
        if (activity == null) return;

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        LinearLayout portalContainer = view.findViewById(R.id.portal_container);
        List<String> discoveredWaypoints = player.getDiscoveredWaypoints();

        // Always show Region 1 entrance
        addPortalButton(portalContainer, activity, 1,
                RoomIdHelper.makeRoomId(1, 0),
                BiomeType.fromRegion(1).getDisplayName() + " - Entrance");

        // Show portal buttons for each discovered waypoint
        for (int region = 1; region <= Constants.NUM_REGIONS; region++) {
            String waypointId = RoomIdHelper.getRegionWaypointId(region);
            if (waypointId != null && discoveredWaypoints.contains(waypointId)) {
                BiomeType biome = BiomeType.fromRegion(region);
                addPortalButton(portalContainer, activity, region, waypointId,
                        biome.getDisplayName() + " - Waypoint");
            }
        }

        // Shop
        view.findViewById(R.id.btn_shop).setOnClickListener(v ->
                activity.showFragment(new ShopFragment(), "shop"));

        // Storage
        view.findViewById(R.id.btn_storage).setOnClickListener(v ->
                activity.showFragment(new TransferFragment(), "transfer"));

        // Trading Post
        view.findViewById(R.id.btn_trading_post).setOnClickListener(v ->
                activity.showFragment(new TradingPostFragment(), "trading_post"));

        // Status
        TextView status = view.findViewById(R.id.hub_status);
        int totalRooms = 0;
        for (List<String> rooms : player.getDiscoveredRooms().values()) {
            totalRooms += rooms.size();
        }
        int waypointCount = discoveredWaypoints.size();
        status.setText("Welcome, " + player.getName() + "! Rooms: " + totalRooms
                + " | Waypoints: " + waypointCount + "/" + Constants.NUM_REGIONS);
    }

    private void addPortalButton(LinearLayout container, GameActivity activity,
                                  int region, String targetRoomId, String label) {
        BiomeType biome = BiomeType.fromRegion(region);
        Button btn = new Button(requireContext());
        btn.setText(label);
        btn.setTextSize(13f);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(50));
        lp.setMargins(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
        btn.setLayoutParams(lp);
        btn.setBackgroundColor(biome.getColor());

        btn.setOnClickListener(v -> activity.navigateToRoom(targetRoomId));
        container.addView(btn);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
