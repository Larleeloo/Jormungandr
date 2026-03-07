package com.larleeloo.jormungandr.model;

public class InventorySlot {
    private String itemId;
    private int quantity;
    private int slot;

    public InventorySlot() {}

    public InventorySlot(String itemId, int quantity, int slot) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.slot = slot;
    }

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty() || quantity <= 0;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
}
