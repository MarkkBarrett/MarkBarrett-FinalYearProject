package com.example.vitalmix.api;

import java.util.Map;

public class ApiResponseFastAPI {
    private boolean success;
    private String message;
    private Map<String, Object> data; // Store nested JSON data

    // Add video_url field separately
    private String video_url;

    // Getters & Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    // Getter for video_url
    public String getVideoUrl() {
        if (data != null && data.containsKey("video_url")) {
            return data.get("video_url").toString();
        }
        return null; // Default to null if not found
    }
}

