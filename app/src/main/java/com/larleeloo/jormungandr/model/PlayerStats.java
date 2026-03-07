package com.larleeloo.jormungandr.model;

public class PlayerStats {
    private int strength;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;
    private int dexterity;
    private int availablePoints;

    public PlayerStats() {
        this.strength = 1;
        this.constitution = 1;
        this.intelligence = 1;
        this.wisdom = 1;
        this.charisma = 1;
        this.dexterity = 1;
        this.availablePoints = 0;
    }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public int getConstitution() { return constitution; }
    public void setConstitution(int constitution) { this.constitution = constitution; }
    public int getIntelligence() { return intelligence; }
    public void setIntelligence(int intelligence) { this.intelligence = intelligence; }
    public int getWisdom() { return wisdom; }
    public void setWisdom(int wisdom) { this.wisdom = wisdom; }
    public int getCharisma() { return charisma; }
    public void setCharisma(int charisma) { this.charisma = charisma; }
    public int getDexterity() { return dexterity; }
    public void setDexterity(int dexterity) { this.dexterity = dexterity; }
    public int getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(int availablePoints) { this.availablePoints = availablePoints; }
}
