package com.example.vitalmix.api;

import com.example.vitalmix.models.DefaultWorkoutPlan;
import com.example.vitalmix.models.Exercise;
import com.example.vitalmix.models.WorkoutSession;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Handles Retrofit setup for API calls
public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:5000"; // Backend server URL for local testing
    private static Retrofit retrofit;
    private static final Gson gson = new GsonBuilder().create(); // Create Gson instance

    // Create and return a Retrofit client
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // Base URL for all API calls
                    .addConverterFactory(GsonConverterFactory.create()) // Gson for JSON parsing
                    .build();
        }
        return retrofit;
    }
    //Instance of interface so I don't have to repeatedly call and create it
    public static ApiService getApiService() {
        // Get the Retrofit client
        return getClient().create(ApiService.class);
    }

    // Returns Gson instance
    public static Gson getGson() {
        return gson;
    }

    // Returns TypeToken for List<Exercise> conversion
    public static Type getExerciseListType() {
        return new TypeToken<List<Exercise>>() {}.getType();
    }

    // Returns TypeToken for List<DefaultWorkoutPlan> conversion
    public static Type getDefaultWorkoutPlanListType() {
        return new TypeToken<List<DefaultWorkoutPlan>>() {}.getType();
    }

    // Returns TypeToken for List<WorkoutSession> conversion
    public static Type getWorkoutSessionListType() {
        return new TypeToken<List<WorkoutSession>>() {}.getType();
    }

}