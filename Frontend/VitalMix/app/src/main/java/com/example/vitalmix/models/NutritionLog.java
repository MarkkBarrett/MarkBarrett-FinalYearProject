package com.example.vitalmix.models;

import java.util.List;

// Stores logged meals per day
public class NutritionLog {
    private String logDate;
    private List<MealLog> mealsLogged; // Meals consumed that day
    private int totalCalories;
    private int totalProtein;
    private int totalCarbs;
    private int totalFats;

    // Empty constructor
    public NutritionLog() {
    }

    // Constructor
    public NutritionLog(String logDate, List<MealLog> mealsLogged, int totalCalories, int totalProtein, int totalCarbs, int totalFats) {
        this.logDate = logDate;
        this.mealsLogged = mealsLogged;
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
        this.totalCarbs = totalCarbs;
        this.totalFats = totalFats;
    }

    // Getters and Setters
}