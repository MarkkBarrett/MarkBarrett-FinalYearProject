package com.example.vitalmix.models;

// Logs an actual meal the user ate
public class MealLog {
    private String mealName;
    private int calories;
    private int protein;
    private int carbs;
    private int fats;

    // Empty constructor
    public MealLog() {
    }

    // Constructor
    public MealLog(String mealName, int calories, int protein, int carbs, int fats) {
        this.mealName = mealName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    // Getters and Setters
}