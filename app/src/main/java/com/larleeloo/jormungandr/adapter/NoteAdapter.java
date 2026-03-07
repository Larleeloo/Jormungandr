package com.larleeloo.jormungandr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.model.PlayerNote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final List<PlayerNote> notes;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US);

    public NoteAdapter(List<PlayerNote> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        PlayerNote note = notes.get(position);
        holder.author.setText(note.getPlayerName());
        holder.text.setText(note.getText());
        holder.time.setText(DATE_FORMAT.format(new Date(note.getTimestamp() * 1000)));
    }

    @Override
    public int getItemCount() { return notes.size(); }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView author, text, time;

        NoteViewHolder(View view) {
            super(view);
            author = view.findViewById(R.id.note_author);
            text = view.findViewById(R.id.note_text);
            time = view.findViewById(R.id.note_time);
        }
    }
}
