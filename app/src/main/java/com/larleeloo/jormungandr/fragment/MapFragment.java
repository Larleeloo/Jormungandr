package com.larleeloo.jormungandr.fragment;

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
import com.larleeloo.jormungandr.util.RoomIdHelper;
import com.larleeloo.jormungandr.view.MapCanvasView;

public class MapFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MapCanvasView mapCanvas = view.findViewById(R.id.map_canvas);
        TextView mapLocation = view.findViewById(R.id.map_location);
        LinearLayout waypointPanel = view.findViewById(R.id.waypoint_panel);
        Button btnTeleportHub = view.findViewById(R.id.btn_teleport_hub);

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();

        if (player != null) {
            String roomId = player.getCurrentRoomId();
            int region = player.getCurrentRegion();
            BiomeType biome = BiomeType.fromRegion(region);

            mapLocation.setText(biome.getDisplayName() + " - " + roomId);
            mapCanvas.setPlayer(player, roomId);

            // Show waypoint panel if player has discovered waypoints
            if (!player.getDiscoveredWaypoints().isEmpty()) {
                waypointPanel.setVisibility(View.VISIBLE);
            }

            btnTeleportHub.setOnClickListener(v -> {
                GameActivity activity = (GameActivity) getActivity();
                if (activity != null) {
                    activity.navigateToRoom("r0_00000");
                }
            });
        }
    }
}
