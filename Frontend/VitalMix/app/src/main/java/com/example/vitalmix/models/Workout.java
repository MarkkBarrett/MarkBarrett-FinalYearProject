package com.example.vitalmix.models;

import java.util.List;

// a workout within the workout plan
public class Workout {
    private String workoutName; //name of workout maybe push1 or pull
    private List<String> exercises; // List of exercise ids in this workout
    //Add sets and reps to this maybe

    // Empty constructor
    public Workout() {
    }

    // Constructor
    public Workout(String workoutName, List<String> exercises) {
        this.workoutName = workoutName;
        this.exercises = exercises;
    }

    // Getters and setters
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
