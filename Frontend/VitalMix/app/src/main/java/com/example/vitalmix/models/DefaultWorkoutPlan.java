package com.example.vitalmix.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// Represents a default workout plan available for all users
public class DefaultWorkoutPlan {
    @SerializedName("_id") // Maps "_id" from backend to id
    private String id; // e.g. "default_beg_hypertrophy"
    private String planName;
    private String description;
    private List<String> focus; // build muscle , general fitness
    private List<Workout> workouts; // List of default workouts in the plan

    // Empty constructor
    public DefaultWorkoutPlan() {
    }

    // Constructor
    public DefaultWorkoutPlan(String id, String planName, String description, List<String> focus, List<Workout> workouts) {
        this.id = id;
        this.planName = planName;
        this.description = description;
        this.focus = focus;
        this.workouts = workouts;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getFocus() {
        return focus;
    }

    public void setFocus(List<String> focus) {
        this.focus = focus;
    }

    public List<Workout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
    }
}