package com.larleeloo.jormungandr.model;

/**
 * A player-to-player trade listing in the hub trading post.
 * Stored in the hub Room's tradeListings list and persisted to Drive.
 */
public class TradeListing {
    private String itemId;
    private int quantity;
    private int price;
    private String sellerName;
    private String sellerAccessCode;
    private long listedAt;

    public TradeListing() {}

    public TradeListing(String itemId, int quantity, int price,
                        String sellerName, String sellerAccessCode) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.price = price;
        this.sellerName = sellerName;
        this.sellerAccessCode = sellerAccessCode;
        this.listedAt = System.currentTimeMillis() / 1000;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSellerAccessCode() { return sellerAccessCode; }
    public void setSellerAccessCode(String sellerAccessCode) { this.sellerAccessCode = sellerAccessCode; }
    public long getListedAt() { return listedAt; }
    public void setListedAt(long listedAt) { this.listedAt = listedAt; }
}
