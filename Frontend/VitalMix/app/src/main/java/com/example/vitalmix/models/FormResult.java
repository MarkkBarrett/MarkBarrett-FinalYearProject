package com.example.vitalmix.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FormResult {
    @SerializedName("_id")
    private String id;          // MongoDB document _id
    private String userId;      // ID of the user who performed the check
    private String exercise;    // name of the exercise checked
    private double accuracy;    // accuracy percentage
    private List<String> feedback;   // feedback messages
    private String timestamp;   // ISO-format timestamp

    // Empty constructor
    public FormResult() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getExercise() { return exercise; }
    public void setExercise(String exercise) { this.exercise = exercise; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public List<String> getFeedback() { return feedback; }
    public void setFeedback(List<String> feedback) { this.feedback = feedback; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}