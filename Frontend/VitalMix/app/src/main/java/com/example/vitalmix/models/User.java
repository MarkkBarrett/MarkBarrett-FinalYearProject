package com.example.vitalmix.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("_id") // Map MongoDB _id to userID in Java
    private String userID; // Store userId from backend
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private boolean hasCompletedQuestionnaire; // Check if questionnare has been done
    private Questionnaire questionnaire; // User's questionnaire responses

    //Empty Constructor
    public User() {
    }

    // Constructor
    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.hasCompletedQuestionnaire = false; // Default value for new users
        this.questionnaire = null; // No questionnaire data at first
    }

    // Getter and setters
    public String getUserID() { return userID; }

    public void setUserID(String userID) { this.userID = userID; }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getHasCompletedQuestionnaire() {
        return hasCompletedQuestionnaire;
    }

    public void setHasCompletedQuestionnaire(boolean hasCompletedQuestionnaire) {
        this.hasCompletedQuestionnaire = hasCompletedQuestionnaire;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public void setQuestionnaire(Questionnaire questionnaire) {
        this.questionnaire = questionnaire;
    }
}