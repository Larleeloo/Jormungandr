package com.larleeloo.jormungandr.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.adapter.NoteAdapter;
import com.larleeloo.jormungandr.cloud.CloudSyncManager;
import com.larleeloo.jormungandr.data.GameRepository;
import com.larleeloo.jormungandr.model.Player;
import com.larleeloo.jormungandr.model.PlayerNote;
import com.larleeloo.jormungandr.model.Room;

import java.util.ArrayList;
import java.util.List;

public class NoteFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText noteInput = view.findViewById(R.id.note_input);
        Button postBtn = view.findViewById(R.id.btn_post_note);
        RecyclerView notesList = view.findViewById(R.id.notes_list);
        notesList.setLayoutManager(new LinearLayoutManager(requireContext()));

        GameRepository repo = GameRepository.getInstance(requireContext());
        Room room = repo.getCurrentRoom();
        Player player = repo.getCurrentPlayer();

        List<PlayerNote> notes = new ArrayList<>();
        if (room != null && room.getPlayerNotes() != null) {
            notes.addAll(room.getPlayerNotes());
        }

        NoteAdapter adapter = new NoteAdapter(notes);
        notesList.setAdapter(adapter);

        postBtn.setOnClickListener(v -> {
            String text = noteInput.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Write something first!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (player == null || room == null) return;

            PlayerNote note = new PlayerNote(player.getName(), text, room.getRoomId());
            room.getPlayerNotes().add(note);
            player.getNotes().add(note);

            repo.saveCurrentRoom();
            repo.savePlayer();

            // Sync notes to cloud
            CloudSyncManager syncManager = new CloudSyncManager();
            syncManager.syncNotesToCloud(room.getRoomId(), room.getPlayerNotes(), null);

            notes.add(note);
            adapter.notifyItemInserted(notes.size() - 1);
            noteInput.setText("");

            Toast.makeText(requireContext(), "Note posted!", Toast.LENGTH_SHORT).show();
        });
    }
}
