package com.example.vitalmix.models;

import java.util.List;

public class WorkoutExerciseLog {
    private String exerciseId; // Link to the exercise performed
    private List<SetLog> sets;
    private float lastUsedWeight;
    private int lastUsedReps;

    // Empty Constructor
    public WorkoutExerciseLog() {
    }

    // Constructor
    public WorkoutExerciseLog(String exerciseId, List<SetLog> sets, float lastUsedWeight, int lastUsedReps) {
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.lastUsedWeight = lastUsedWeight;
        this.lastUsedReps = lastUsedReps;
    }

    // Getters and Setters
    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public List<SetLog> getSets() {
        return sets;
    }

    public void setSets(List<SetLog> sets) {
        this.sets = sets;
    }

    public float getLastUsedWeight() {
        return lastUsedWeight;
    }

    public void setLastUsedWeight(float lastUsedWeight) {
        this.lastUsedWeight = lastUsedWeight;
    }

    public int getLastUsedReps() {
        return lastUsedReps;
    }

    public void setLastUsedReps(int lastUsedReps) {
        this.lastUsedReps = lastUsedReps;
    }
}
