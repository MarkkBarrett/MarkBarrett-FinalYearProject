package com.example.vitalmix.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// a workout within the workout plan
public class Workout {
    @SerializedName("_id") // Maps the MongoDB ID when present
    private String id;
    private String workoutName; //name of workout maybe push1 or pull
    private List<String> exercises; // List of exercise ids in this workout
    //Add sets and reps to this maybe

    // Empty constructor
    public Workout() {
    }

    // Constructor
    public Workout(String id, String workoutName, List<String> exercises) {
        this.id = id;
        this.workoutName = workoutName;
        this.exercises = exercises;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public List<String> getExercises() {
        return exercises;
    }

    public void setExercises(List<String> exercises) {
        this.exercises = exercises;
    }
}
