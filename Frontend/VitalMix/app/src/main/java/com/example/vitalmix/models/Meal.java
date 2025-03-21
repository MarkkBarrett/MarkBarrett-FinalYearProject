package com.example.vitalmix.models;

// meal suggestion in the nutrition plan
public class Meal {
    private String mealTime; // Breakfast, Lunch, etc.
    private String mealName;
    private int calories;
    private int protein;
    private int carbs;
    private int fats;

    // Empty constructor
    public Meal() {
    }

    // Constructor
    public Meal(String mealTime, String mealName, int calories, int protein, int carbs, int fats) {
        this.mealTime = mealTime;
        this.mealName = mealName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    // Getters and Setters
}
