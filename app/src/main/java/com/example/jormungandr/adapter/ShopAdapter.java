package com.example.jormungandr.adapter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jormungandr.R;
import com.example.jormungandr.model.ItemDef;
import com.example.jormungandr.view.PlaceholderRenderer;

import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    public interface OnBuyClickListener {
        void onBuy(ItemDef item);
    }

    private final List<ItemDef> items;
    private final OnBuyClickListener listener;

    public ShopAdapter(List<ItemDef> items, OnBuyClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        ItemDef item = items.get(position);

        Bitmap bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        PlaceholderRenderer.drawShape(canvas, item.getPlaceholderShape(),
                item.getPlaceholderColorInt(), 2, 2, 44, 44);
        holder.icon.setImageBitmap(bmp);

        holder.name.setText(item.getDisplayName());
        holder.name.setTextColor(item.getRarityEnum().getGlowColor());
        holder.desc.setText(item.getRarityEnum().getDisplayName() + " | " + item.getBuyPrice() + "g");
        holder.buyBtn.setText("Buy " + item.getBuyPrice() + "g");
        holder.buyBtn.setOnClickListener(v -> listener.onBuy(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ShopViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, desc;
        Button buyBtn;

        ShopViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.shop_item_icon);
            name = view.findViewById(R.id.shop_item_name);
            desc = view.findViewById(R.id.shop_item_desc);
            buyBtn = view.findViewById(R.id.btn_buy);
        }
    }
}
