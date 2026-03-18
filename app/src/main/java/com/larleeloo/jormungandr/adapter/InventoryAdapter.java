package com.larleeloo.jormungandr.adapter;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.asset.GameAssetManager;
import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.view.PlaceholderRenderer;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.SlotViewHolder> {

    public interface OnSlotClickListener {
        void onSlotClick(int position, InventorySlot slot);
    }

    public interface OnSlotMoveListener {
        void onSlotMove(int fromPosition, int toPosition);
    }

    private final List<InventorySlot> inventory;
    private final ItemRegistry itemRegistry;
    private final OnSlotClickListener listener;
    private OnSlotMoveListener moveListener;
    private int selectedPosition = -1;

    public InventoryAdapter(List<InventorySlot> inventory, ItemRegistry itemRegistry,
                            OnSlotClickListener listener) {
        this.inventory = inventory;
        this.itemRegistry = itemRegistry;
        this.listener = listener;
    }

    public void setOnSlotMoveListener(OnSlotMoveListener moveListener) {
        this.moveListener = moveListener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        InventorySlot slot = inventory.get(position);

        if (slot.isEmpty()) {
            holder.slotIcon.setImageBitmap(null);
            holder.slotIcon.setImageDrawable(null);
            holder.slotQuantity.setText("");
            holder.slotContainer.setBackgroundResource(R.drawable.inventory_slot_background);
        } else {
            ItemDef item = itemRegistry.getItem(slot.getItemId());
            if (item != null) {
                // Try loading actual sprite, fall back to placeholder
                Bitmap spriteBmp = null;
                if (item.getSpritePath() != null) {
                    spriteBmp = GameAssetManager.getInstance(holder.itemView.getContext())
                            .loadSprite(item.getSpritePath());
                }
                Bitmap bmp;
                if (spriteBmp != null) {
                    bmp = Bitmap.createScaledBitmap(spriteBmp, 64, 64, true);
                } else {
                    bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmp);
                    PlaceholderRenderer.drawShape(canvas, item.getPlaceholderShape(),
                            item.getPlaceholderColorInt(), 4, 4, 56, 56);
                }
                // Add rarity glow
                Canvas glowCanvas = new Canvas(bmp);
                PlaceholderRenderer.drawRarityGlow(glowCanvas, item.getRarityEnum().getGlowColor(),
                        2, 2, 60, 60, 3);
                holder.slotIcon.setImageBitmap(bmp);
                holder.slotQuantity.setText(slot.getQuantity() > 1 ?
                        String.valueOf(slot.getQuantity()) : "");
                holder.slotQuantity.setTextColor(item.getRarityEnum().getGlowColor());
            }

            holder.slotContainer.setBackgroundResource(
                    position == selectedPosition ?
                            R.drawable.inventory_slot_selected :
                            R.drawable.inventory_slot_background);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            if (oldSelected >= 0) notifyItemChanged(oldSelected);
            notifyItemChanged(selectedPosition);
            listener.onSlotClick(selectedPosition, slot);
        });

        // Long-press starts drag for non-empty slots
        holder.itemView.setOnLongClickListener(v -> {
            if (slot.isEmpty()) return false;
            int pos = holder.getAdapterPosition();
            ClipData data = new ClipData("inventory_slot",
                    new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN},
                    new ClipData.Item(String.valueOf(pos)));
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            // Pass the position as an Integer so we can identify the source slot
            v.startDragAndDrop(data, shadow, Integer.valueOf(pos), 0);
            return true;
        });

        // Each slot is a drop target for inventory-to-inventory moves
        holder.itemView.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Accept drags that originated from inventory slots
                    return event.getLocalState() instanceof Integer;
                case DragEvent.ACTION_DRAG_ENTERED:
                    holder.slotContainer.setBackgroundResource(R.drawable.inventory_slot_selected);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    int currentPos = holder.getAdapterPosition();
                    holder.slotContainer.setBackgroundResource(
                            currentPos == selectedPosition ?
                                    R.drawable.inventory_slot_selected :
                                    R.drawable.inventory_slot_background);
                    return true;
                case DragEvent.ACTION_DROP:
                    int targetPos = holder.getAdapterPosition();
                    if (targetPos < 0) return false;
                    Object localState = event.getLocalState();
                    if (localState instanceof Integer) {
                        int fromPos = (Integer) localState;
                        if (fromPos != targetPos && moveListener != null) {
                            moveListener.onSlotMove(fromPos, targetPos);
                        }
                    }
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    // Reset background after drag completes
                    int endPos = holder.getAdapterPosition();
                    if (endPos >= 0) {
                        holder.slotContainer.setBackgroundResource(
                                endPos == selectedPosition ?
                                        R.drawable.inventory_slot_selected :
                                        R.drawable.inventory_slot_background);
                    }
                    return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return inventory.size();
    }

    public void clearSelection() {
        int old = selectedPosition;
        selectedPosition = -1;
        if (old >= 0) notifyItemChanged(old);
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        LinearLayout slotContainer;
        ImageView slotIcon;
        TextView slotQuantity;

        SlotViewHolder(View itemView) {
            super(itemView);
            slotContainer = itemView.findViewById(R.id.slot_container);
            slotIcon = itemView.findViewById(R.id.slot_icon);
            slotQuantity = itemView.findViewById(R.id.slot_quantity);
        }
    }
}
