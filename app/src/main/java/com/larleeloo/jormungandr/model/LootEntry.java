package com.larleeloo.jormungandr.model;

public class LootEntry {
    private String itemId;
    private double weight;

    public LootEntry() {}

    public LootEntry(String itemId, double weight) {
        this.itemId = itemId;
        this.weight = weight;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
