package com.jostein.maratontrener.models;

import java.util.List;

public class Meal {
    private final String id;
    private final String title;
    private final String type; // "Frokost", "Lunsj", "Middag"
    private final int weekNumber; // 1 to 12
    private final List<Ingredient> ingredients;
    private final String instructions;
    private final double baseCalories;
    private final double baseProtein;
    private final double baseCarbs;
    private final double baseFat;
    private final String advice; // Compliant with Norwegian government's Kostrådene
    private final String emoji;

    public Meal(String id, String title, String type, int weekNumber, List<Ingredient> ingredients, 
                String instructions, double baseCalories, double baseProtein, double baseCarbs, 
                double baseFat, String advice, String emoji) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.weekNumber = weekNumber;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.baseCalories = baseCalories;
        this.baseProtein = baseProtein;
        this.baseCarbs = baseCarbs;
        this.baseFat = baseFat;
        this.advice = advice;
        this.emoji = emoji;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public double getBaseCalories() {
        return baseCalories;
    }

    public double getBaseProtein() {
        return baseProtein;
    }

    public double getBaseCarbs() {
        return baseCarbs;
    }

    public double getBaseFat() {
        return baseFat;
    }

    public String getAdvice() {
        return advice;
    }

    public String getEmoji() {
        return emoji;
    }
}
