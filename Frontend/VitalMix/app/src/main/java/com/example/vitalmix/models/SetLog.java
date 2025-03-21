package com.example.vitalmix.models;

// Logs individual set data (weight, reps)
public class SetLog {
    private int setNumber;
    private int reps;
    private float weight;

    // Empty constructor
    public SetLog() {
    }

    // Constructor
    public SetLog(int setNumber, int reps, float weight) {
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
    }

    // Getters and Setters
    public int getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}