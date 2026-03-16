package com.larleeloo.jormungandr.adapter;

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

import com.larleeloo.jormungandr.R;
import com.larleeloo.jormungandr.asset.GameAssetManager;
import com.larleeloo.jormungandr.data.ItemRegistry;
import com.larleeloo.jormungandr.model.ItemDef;
import com.larleeloo.jormungandr.model.TradeListing;
import com.larleeloo.jormungandr.view.PlaceholderRenderer;

import java.util.List;

public class TradeListingAdapter extends RecyclerView.Adapter<TradeListingAdapter.TradeViewHolder> {

    public interface OnTradeActionListener {
        void onBuy(int position, TradeListing listing);
        void onCancel(int position, TradeListing listing);
    }

    private final List<TradeListing> listings;
    private final ItemRegistry itemRegistry;
    private final String currentPlayerAccessCode;
    private final OnTradeActionListener listener;

    public TradeListingAdapter(List<TradeListing> listings, ItemRegistry itemRegistry,
                               String currentPlayerAccessCode, OnTradeActionListener listener) {
        this.listings = listings;
        this.itemRegistry = itemRegistry;
        this.currentPlayerAccessCode = currentPlayerAccessCode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trade_listing, parent, false);
        return new TradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TradeViewHolder holder, int position) {
        TradeListing listing = listings.get(position);
        ItemDef item = itemRegistry.getItem(listing.getItemId());

        // Icon
        Bitmap spriteBmp = null;
        if (item != null && item.getSpritePath() != null) {
            spriteBmp = GameAssetManager.getInstance(holder.itemView.getContext())
                    .loadSprite(item.getSpritePath());
        }
        Bitmap bmp;
        if (spriteBmp != null) {
            bmp = Bitmap.createScaledBitmap(spriteBmp, 48, 48, true);
        } else if (item != null) {
            bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            PlaceholderRenderer.drawShape(canvas, item.getPlaceholderShape(),
                    item.getPlaceholderColorInt(), 2, 2, 44, 44);
        } else {
            bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        }
        holder.icon.setImageBitmap(bmp);

        // Text
        String displayName = item != null ? item.getDisplayName() : listing.getItemId();
        holder.name.setText(displayName + (listing.getQuantity() > 1 ? " x" + listing.getQuantity() : ""));
        if (item != null) {
            holder.name.setTextColor(item.getRarityEnum().getGlowColor());
        }
        holder.seller.setText("Seller: " + listing.getSellerName());
        holder.price.setText(listing.getPrice() + "g");

        // Show Buy or Cancel depending on whether this is the current player's listing
        boolean isOwnListing = currentPlayerAccessCode.equals(listing.getSellerAccessCode());
        if (isOwnListing) {
            holder.actionBtn.setText("Cancel");
            holder.actionBtn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF8B0000));
            holder.actionBtn.setOnClickListener(v -> listener.onCancel(holder.getAdapterPosition(), listing));
        } else {
            holder.actionBtn.setText("Buy " + listing.getPrice() + "g");
            holder.actionBtn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF228B22));
            holder.actionBtn.setOnClickListener(v -> listener.onBuy(holder.getAdapterPosition(), listing));
        }
    }

    @Override
    public int getItemCount() { return listings.size(); }

    static class TradeViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, seller, price;
        Button actionBtn;

        TradeViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.trade_item_icon);
            name = view.findViewById(R.id.trade_item_name);
            seller = view.findViewById(R.id.trade_item_seller);
            price = view.findViewById(R.id.trade_item_price);
            actionBtn = view.findViewById(R.id.btn_trade_action);
        }
    }
}
