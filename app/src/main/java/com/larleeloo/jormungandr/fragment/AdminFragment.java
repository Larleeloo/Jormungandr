package com.larleeloo.jormungandr.fragment;

import android.app.AlertDialog;
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
import com.larleeloo.jormungandr.cloud.AccessCodeValidator;
import com.larleeloo.jormungandr.cloud.CloudSyncManager;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.engine.MeshExporter;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.util.Constants;

public class AdminFragment extends Fragment {

    private CloudSyncManager cloudSyncManager;
    private TextView adminStatus;
    private Button btnResetRooms, btnResetNotes, btnResetPlayers, btnExportMesh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cloudSyncManager = new CloudSyncManager();

        adminStatus = view.findViewById(R.id.admin_status);
        btnResetRooms = view.findViewById(R.id.btn_reset_rooms);
        btnResetNotes = view.findViewById(R.id.btn_reset_notes);
        btnResetPlayers = view.findViewById(R.id.btn_reset_players);
        btnExportMesh = view.findViewById(R.id.btn_export_mesh);

        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null || !AccessCodeValidator.isAdminCode(player.getAccessCode())) {
            disableAllButtons();
            showStatus("Unauthorized. Admin access required.", false);
            return;
        }

        String accessCode = player.getAccessCode();

        btnResetRooms.setOnClickListener(v -> confirmAndExecute(
                "Reset All Rooms",
                "This will delete ALL room data from the cloud. All players currently in rooms will be sent back to the spawn hub.\n\nAre you sure?",
                () -> resetAllRooms(accessCode)
        ));

        btnResetNotes.setOnClickListener(v -> confirmAndExecute(
                "Reset All Notes",
                "This will delete ALL player notes from the cloud.\n\nAre you sure?",
                () -> resetAllNotes(accessCode)
        ));

        btnResetPlayers.setOnClickListener(v -> confirmAndExecute(
                "Reset All Player Saves",
                "This will delete ALL player save data from the cloud. Every player will need to create a new character.\n\nAre you sure?",
                () -> resetAllPlayers(accessCode)
        ));

        btnExportMesh.setOnClickListener(v -> confirmAndExecute(
                "Export World Mesh",
                "This will generate the full 80,000-room mesh reference and upload it to Drive as a single JSON file.\n\nThis is a one-time operation. Continue?",
                this::exportMesh
        ));
    }

    private void confirmAndExecute(String title, String message, Runnable action) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Reset", (dialog, which) -> action.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetAllRooms(String accessCode) {
        disableAllButtons();
        showStatus("Resetting all room data...", true);

        cloudSyncManager.adminResetAllRooms(accessCode, (success, message) -> {
            showStatus(message, success);
            enableAllButtons();

            if (success) {
                sendPlayerToHub();
            }
        });
    }

    private void resetAllNotes(String accessCode) {
        disableAllButtons();
        showStatus("Resetting all note data...", true);

        cloudSyncManager.adminResetAllNotes(accessCode, (success, message) -> {
            showStatus(message, success);
            enableAllButtons();
        });
    }

    private void resetAllPlayers(String accessCode) {
        disableAllButtons();
        showStatus("Resetting all player save data...", true);

        cloudSyncManager.adminResetAllPlayers(accessCode, (success, message) -> {
            showStatus(message, success);
            enableAllButtons();
        });
    }

    private void exportMesh() {
        disableAllButtons();
        showStatus("Generating mesh reference (this may take a moment)...", true);

        MeshExporter.exportAndUpload(requireContext(), (success, message) -> {
            showStatus(message, success);
            enableAllButtons();
        });
    }

    private void sendPlayerToHub() {
        GameRepository repo = GameRepository.getInstance(requireContext());
        Player player = repo.getCurrentPlayer();
        if (player == null) return;

        player.setRoomsVisitedSinceHub(0);
        player.getRoomHistory().clear();
        repo.navigateToRoom(Constants.HUB_ROOM_ID);
        repo.savePlayer();

        GameActivity activity = (GameActivity) getActivity();
        if (activity != null) {
            activity.updateHud();
            activity.showFragment(new RoomFragment(), "room");
            Toast.makeText(requireContext(), "Rooms reset. Returned to spawn hub.", Toast.LENGTH_LONG).show();
        }
    }

    private void showStatus(String message, boolean success) {
        if (adminStatus == null) return;
        adminStatus.setVisibility(View.VISIBLE);
        adminStatus.setText(message);
        if (message.contains("...")) {
            adminStatus.setTextColor(0xFFAAAAAA);
        } else if (success) {
            adminStatus.setTextColor(0xFF44FF44);
        } else {
            adminStatus.setTextColor(0xFFFF4444);
        }
    }

    private void disableAllButtons() {
        btnResetRooms.setEnabled(false);
        btnResetNotes.setEnabled(false);
        btnResetPlayers.setEnabled(false);
        btnExportMesh.setEnabled(false);
    }

    private void enableAllButtons() {
        btnResetRooms.setEnabled(true);
        btnResetNotes.setEnabled(true);
        btnResetPlayers.setEnabled(true);
        btnExportMesh.setEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cloudSyncManager != null) {
            cloudSyncManager.shutdown();
        }
    }
}
