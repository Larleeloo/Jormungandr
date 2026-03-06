package com.example.jormungandr.adapter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jormungandr.R;
import com.example.jormungandr.model.ActionType;
import com.example.jormungandr.model.ItemDef;
import com.example.jormungandr.view.PlaceholderRenderer;

import java.util.List;

public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ActionViewHolder> {

    public static class CombatAction {
        public final ItemDef item;
        public final ActionType actionType;

        public CombatAction(ItemDef item, ActionType actionType) {
            this.item = item;
            this.actionType = actionType;
        }
    }

    public interface OnActionClickListener {
        void onActionClick(CombatAction action);
    }

    private final List<CombatAction> actions;
    private final OnActionClickListener listener;

    public ActionAdapter(List<CombatAction> actions, OnActionClickListener listener) {
        this.actions = actions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        CombatAction action = actions.get(position);

        // Draw item icon
        Bitmap bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        PlaceholderRenderer.drawShape(canvas, action.item.getPlaceholderShape(),
                action.item.getPlaceholderColorInt(), 2, 2, 44, 44);
        holder.actionIcon.setImageBitmap(bmp);

        holder.itemName.setText(action.item.getDisplayName());
        holder.itemName.setTextColor(action.item.getRarityEnum().getGlowColor());
        holder.actionTypeName.setText(action.actionType.getDisplayName());

        if (action.actionType.isCombatAction() && action.item.getDamage() > 0) {
            holder.actionDamage.setText("DMG:" + action.item.getDamage());
            holder.actionDamage.setVisibility(View.VISIBLE);
        } else if (action.item.getHealAmount() > 0) {
            holder.actionDamage.setText("+" + action.item.getHealAmount() + "HP");
            holder.actionDamage.setTextColor(0xFF00CC00);
            holder.actionDamage.setVisibility(View.VISIBLE);
        } else {
            holder.actionDamage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onActionClick(action));
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    static class ActionViewHolder extends RecyclerView.ViewHolder {
        ImageView actionIcon;
        TextView itemName, actionTypeName, actionDamage;

        ActionViewHolder(View view) {
            super(view);
            actionIcon = view.findViewById(R.id.action_icon);
            itemName = view.findViewById(R.id.action_item_name);
            actionTypeName = view.findViewById(R.id.action_type_name);
            actionDamage = view.findViewById(R.id.action_damage);
        }
    }
}
