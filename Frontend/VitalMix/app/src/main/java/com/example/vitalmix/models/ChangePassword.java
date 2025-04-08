package com.example.vitalmix.models;

import com.google.gson.annotations.SerializedName;

public class ChangePassword {
    @SerializedName("_id") // Map MongoDB _id to userID in Java
    private String userId;
    private String currentPassword;
    private String newPassword;

    public ChangePassword(String userId, String currentPassword, String newPassword) {
        this.userId = userId;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
