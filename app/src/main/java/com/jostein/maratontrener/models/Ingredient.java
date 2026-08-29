package com.jostein.maratontrener.models;

public class Ingredient {
    private final String name;
    private final double baseAmount; // Base amount for 70kg runner
    private final String unit; // "g", "dl", "stk", "ss" etc

    public Ingredient(String name, double baseAmount, String unit) {
        this.name = name;
        this.baseAmount = baseAmount;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public String getUnit() {
        return unit;
    }
}
