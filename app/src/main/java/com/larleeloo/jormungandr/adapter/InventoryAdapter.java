package com.larleeloo.jormungandr.adapter;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.model.InventorySlot;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.view.PlaceholderRenderer;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.SlotViewHolder> {

    public interface OnSlotClickListener {
        void onSlotClick(int position, InventorySlot slot);
    }

    private final List<InventorySlot> inventory;
    private final ItemRegistry itemRegistry;
    private final OnSlotClickListener listener;
    private int selectedPosition = -1;

    public InventoryAdapter(List<InventorySlot> inventory, ItemRegistry itemRegistry,
                            OnSlotClickListener listener) {
        this.inventory = inventory;
        this.itemRegistry = itemRegistry;
        this.listener = listener;
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
                // Draw placeholder icon
                Bitmap bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                PlaceholderRenderer.drawShape(canvas, item.getPlaceholderShape(),
                        item.getPlaceholderColorInt(), 4, 4, 56, 56);
                // Add rarity glow
                PlaceholderRenderer.drawRarityGlow(canvas, item.getRarityEnum().getGlowColor(),
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
            v.startDragAndDrop(data, shadow, slot, 0);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return inventory.size();
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
