package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.activity.GameActivity;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.RoomIdHelper;

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

        // Region buttons
        int[] regionBtnIds = {R.id.btn_region_1, R.id.btn_region_2, R.id.btn_region_3,
                R.id.btn_region_4, R.id.btn_region_5, R.id.btn_region_6,
                R.id.btn_region_7, R.id.btn_region_8};

        for (int i = 0; i < regionBtnIds.length; i++) {
            int region = i + 1;
            Button btn = view.findViewById(regionBtnIds[i]);
            btn.setOnClickListener(v -> {
                String roomId = RoomIdHelper.makeRoomId(region, 0);
                activity.navigateToRoom(roomId);
            });
        }

        // Shop
        view.findViewById(R.id.btn_shop).setOnClickListener(v ->
                activity.showFragment(new ShopFragment(), "shop"));

        // Storage (transfer fragment)
        view.findViewById(R.id.btn_storage).setOnClickListener(v ->
                activity.showFragment(new TransferFragment(), "transfer"));

        // Save
        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            GameRepository repo = GameRepository.getInstance(requireContext());
            repo.savePlayer();
            repo.saveCurrentRoom();
            Toast.makeText(requireContext(), "Game saved!", Toast.LENGTH_SHORT).show();
        });

        // Status
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        TextView status = view.findViewById(R.id.hub_status);
        if (player != null) {
            int totalRooms = 0;
            for (java.util.List<String> rooms : player.getDiscoveredRooms().values()) {
                totalRooms += rooms.size();
            }
            status.setText("Welcome, " + player.getName() + "! Rooms discovered: " + totalRooms);
        }
    }
}
