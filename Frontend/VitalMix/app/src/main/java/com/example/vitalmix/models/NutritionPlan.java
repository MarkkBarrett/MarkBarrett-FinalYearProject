package com.example.vitalmix.models;

import java.util.List;

// Stores the user's daily calorie and macro targets
public class NutritionPlan {
    private int targetCalories;
    private int targetProtein;
    private int targetCarbs;
    private int targetFats;
    private List<Meal> meals; // List of suggested meals

    // Empty constructor
    public NutritionPlan() {
    }

    // Constructor
    public NutritionPlan(int targetCalories, int targetProtein, int targetCarbs, int targetFats, List<Meal> meals) {
        this.targetCalories = targetCalories;
        this.targetProtein = targetProtein;
        this.targetCarbs = targetCarbs;
        this.targetFats = targetFats;
        this.meals = meals;
    }

    // Getters and Setters
}
