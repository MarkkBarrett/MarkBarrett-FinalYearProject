package com.example.vitalmix.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String USER_ID_KEY = "userID"; // Store userId
    private static final String USER_EMAIL_KEY = "user_email"; // Store email
    private static final String USER_FIRST_NAME_KEY = "user_first_name"; // Store first name

    // Save userId
    public static void saveLoggedInUserID(Context context, String userID) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_ID_KEY, userID);
        editor.apply();

        Log.d("SESSION_MANAGER", "UserID saved: " + userID); // Debugging log
    }

    // Retrieve userId
    public static String getLoggedInUserID(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString(USER_ID_KEY, null);
    }

    public static void saveLoggedInUserEmail(Context context, String email) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_EMAIL_KEY, email);
        editor.apply();
    }

    public static String getLoggedInUserEmail(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString(USER_EMAIL_KEY, null);
    }

    // Save first name
    public static void saveLoggedInUserFirstName(Context context, String firstName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USER_FIRST_NAME_KEY, firstName);
        editor.apply();
    }

    // Retrieve first name
    public static String getLoggedInUserFirstName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString(USER_FIRST_NAME_KEY, null);
    }

    // Clear all saved session data
    public static void clearSession(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("VitalMixPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Remove all keys
        editor.apply();
    }

}