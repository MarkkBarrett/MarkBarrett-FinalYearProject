package com.example.vitalmix.api;

import com.example.vitalmix.models.User;

// generic response class for API responses
public class ApiResponse {
    private boolean success;  // was the request successful
    private String message;   // A message from the server
    private String redirectTo; // The redirection target for backend
    private Object data;      // Additional data returned by the server
    private User user; // User object to capture response
    private int nextDayIndex; // store the next workout day index

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRedirectTo() {
        return redirectTo;
    }

    public void setRedirectTo(String redirectTo) {
        this.redirectTo = redirectTo;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) { this.data = data; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }

    public int getNextDayIndex() { return nextDayIndex; }
}