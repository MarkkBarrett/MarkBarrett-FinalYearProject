package com.example.vitalmix.models;

import java.util.List;

// Stores a user's overall workout plan (contains multiple workouts)
public class WorkoutPlan {
    private String userId; // Link to the user who owns this plan
    private String planName;
    private List<Workout> workouts; // List of workouts in the plan
    private int currentDayIndex; // Tracks the current day in the workout plan

    // Empty constructor
    public WorkoutPlan() {
    }

    // Constructor
    public WorkoutPlan(String userId, String planName, List<Workout> workouts) {
        this.userId = userId;
        this.planName = planName;
        this.workouts = workouts;
        this.currentDayIndex = 0; // Default to the first workout in the plan
    }

    // Getters and setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public List<Workout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<Workout> workouts) {
        this.workouts = workouts;
    }

    public int getCurrentDayIndex() {
        return currentDayIndex;
    }

    public void setCurrentDayIndex(int currentDayIndex) {
        this.currentDayIndex = currentDayIndex;
    }

    // Increment to the next day in the workout plan
    public void goToNextWorkout() {
        currentDayIndex = (currentDayIndex + 1) % workouts.size(); // Loops back to the start
    }
}
