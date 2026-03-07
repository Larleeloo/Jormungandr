package com.larleeloo.jormungandr.model;

public class BuffEffect {
    private String name;
    private String stat; // "attack", "defense", "hp", "mana", "stamina"
    private int modifier;
    private int turnsRemaining;
    private String source; // item that created this buff

    public BuffEffect() {}

    public BuffEffect(String name, String stat, int modifier, int turnsRemaining, String source) {
        this.name = name;
        this.stat = stat;
        this.modifier = modifier;
        this.turnsRemaining = turnsRemaining;
        this.source = source;
    }

    public boolean isExpired() {
        return turnsRemaining <= 0;
    }

    public void tick() {
        if (turnsRemaining > 0) turnsRemaining--;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStat() { return stat; }
    public void setStat(String stat) { this.stat = stat; }
    public int getModifier() { return modifier; }
    public void setModifier(int modifier) { this.modifier = modifier; }
    public int getTurnsRemaining() { return turnsRemaining; }
    public void setTurnsRemaining(int turnsRemaining) { this.turnsRemaining = turnsRemaining; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
