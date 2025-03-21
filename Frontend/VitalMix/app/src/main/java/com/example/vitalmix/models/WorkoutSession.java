package com.example.vitalmix.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WorkoutSession {
    @SerializedName("_id") // Map MongoDB _id to userID in Java
    private String userId; // Link to user
    private String sessionDate; // Date workout was completed
    private String workoutName;
    private List<WorkoutExerciseLog> exerciseLogs;

    // Empty Constructor
    public WorkoutSession() {
    }

    // Constructor
    public WorkoutSession(String userId, String sessionDate, String workoutName, List<WorkoutExerciseLog> exerciseLogs) {
        this.userId = userId;
        this.sessionDate = sessionDate;
        this.workoutName = workoutName;
        this.exerciseLogs = exerciseLogs;
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public List<WorkoutExerciseLog> getExerciseLogs() {
        return exerciseLogs;
    }

    public void setExerciseLogs(List<WorkoutExerciseLog> exerciseLogs) {
        this.exerciseLogs = exerciseLogs;
    }
}


