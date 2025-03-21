package com.example.vitalmix.models;

// Stores the user's fitness questionnaire responses
public class Questionnaire {
    private String gender;
    private int age;
    private int height;
    private int weight;
    private String fitnessGoals;
    private String activityLevel;
    private String dietaryInfo;

    //Empty constructor
    public Questionnaire() {
    }

    // Constructor
    public Questionnaire(String gender, int age, int height, int weight, String fitnessGoals, String activityLevel, String dietaryInfo) {
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.fitnessGoals = fitnessGoals;
        this.activityLevel = activityLevel;
        this.dietaryInfo = dietaryInfo;
    }

    // Getters & setters
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getFitnessGoals() {
        return fitnessGoals;
    }

    public void setFitnessGoals(String fitnessGoals) {
        this.fitnessGoals = fitnessGoals;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getDietaryInfo() {
        return dietaryInfo;
    }

    public void setDietaryInfo(String dietaryInfo) {
        this.dietaryInfo = dietaryInfo;
    }
}