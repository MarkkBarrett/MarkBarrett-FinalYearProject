package com.example.vitalmix.models;

import java.util.List;

// Represents an exercise from the database
public class Exercise {
    private String _id; // MongoDB ObjectID for the exercise
    private String name; // Exercise name
    private List<String> targetMuscles; // List of target muscles
    private String exerciseType; // Push, Pull, Legs
    private String equipment; // Dumbbell, Barbell, Machine, etc.
    private String mechanics; // Compound or Isolation
    private String instructions; // Exercise instructions

    // Empty constructor
    public Exercise() {
    }

    // Constructor
    public Exercise(String _id, String name, List<String> targetMuscles, String exerciseType,
                    String equipment, String mechanics, String instructions) {
        this._id = _id;
        this.name = name;
        this.targetMuscles = targetMuscles;
        this.exerciseType = exerciseType;
        this.equipment = equipment;
        this.mechanics = mechanics;
        this.instructions = instructions;
    }

    // Getters and Setters
    public String getId() {
        return _id;
    }

    public void setId(String _id) {
        this._id = _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTargetMuscles() {
        return targetMuscles;
    }

    public void setTargetMuscles(List<String> targetMuscles) {
        this.targetMuscles = targetMuscles;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getMechanics() {
        return mechanics;
    }

    public void setMechanics(String mechanics) {
        this.mechanics = mechanics;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}