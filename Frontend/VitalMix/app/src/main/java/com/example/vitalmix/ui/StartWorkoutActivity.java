package com.example.vitalmix.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vitalmix.R;
import com.example.vitalmix.adapters.ExerciseAdapter;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.models.Exercise;
import com.example.vitalmix.models.WorkoutExerciseLog;
import com.example.vitalmix.models.WorkoutPlan;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.models.WorkoutSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartWorkoutActivity extends AppCompatActivity {

    private RecyclerView exerciseRecyclerView;
    private ExerciseAdapter exerciseAdapter;
    private ApiService apiService;
    private String userId; // = "6790a6c0b1ddfc94b8165b7f"; // Hardcoded for testing
    private WorkoutPlan workoutPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_workout);

        // Initialize API service for backend calls
        apiService = ApiClient.getApiService();

        // Set up RecyclerView for exercises
        exerciseRecyclerView = findViewById(R.id.exercise_recycler_view);
        exerciseRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // get userID from session
        userId = SessionManager.getLoggedInUserID(this);

        // Fetch user's workout plan from backend
        fetchWorkoutPlan();

        // Initialize bottom navigation
        setupBottomNavigation();

        // Finish workout button
        Button finishWorkoutBtn = findViewById(R.id.finish_workout_btn);
        finishWorkoutBtn.setOnClickListener(v -> saveWorkoutSession());
    }

    // Fetches the user's assigned workout plan from backend
    private void fetchWorkoutPlan() {
        Log.d("DEBUG", "Fetching workout plan for userId: " + userId);

        apiService.getWorkoutPlan(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d("DEBUG", "Workout plan fetched successfully");

                    // Convert response JSON into WorkoutPlan object
                    workoutPlan = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()), WorkoutPlan.class
                    );

                    Log.d("DEBUG", "Workout plan details: " + workoutPlan.getPlanName());

                    // Display the workout
                    displayWorkout();
                } else {
                    Log.e("API_ERROR", "Failed to fetch workout plan");
                    Toast.makeText(StartWorkoutActivity.this, "Failed to load workout plan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Workout plan request failed: " + t.getMessage());
                Toast.makeText(StartWorkoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Displays the current workout based on `currentDayIndex`
    private void displayWorkout() {
        if (workoutPlan == null || workoutPlan.getWorkouts().isEmpty()) {
            Toast.makeText(this, "No workouts found", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentDay = workoutPlan.getCurrentDayIndex();
        List<String> exerciseIds = workoutPlan.getWorkouts().get(currentDay).getExercises();

        // Log retrieved exercise IDs for debugging
        Log.d("DEBUG", "Current workout day: " + currentDay);
        Log.d("DEBUG", "Exercise IDs: " + exerciseIds.toString());

        // Fetch full exercise details using IDs
        fetchExerciseDetails(exerciseIds);
    }

    // Fetches detailed exercise data from API based on exercise IDs
    private void fetchExerciseDetails(List<String> exerciseIds) {
        apiService.getExercisesByIds(exerciseIds).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d("DEBUG", "Exercises fetched successfully");

                    List<Exercise> exercises = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()),
                            ApiClient.getExerciseListType()
                    );

                    Log.d("DEBUG", "Number of exercises retrieved: " + exercises.size());

                    if (exercises == null || exercises.isEmpty()) {
                        Log.e("DEBUG", "Exercises fetched from API are null or empty!");
                        return;
                    }

                    // Attach exercises to adapter
                    exerciseAdapter = new ExerciseAdapter(StartWorkoutActivity.this, exercises);
                    exerciseRecyclerView.setAdapter(exerciseAdapter);
                } else {
                    Log.e("API_ERROR", "Failed to fetch exercises");
                    Toast.makeText(StartWorkoutActivity.this, "Failed to load exercises", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Exercise request failed: " + t.getMessage());
                Toast.makeText(StartWorkoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to save the workout session and move to the next workout
    private void saveWorkoutSession() {
        if (exerciseAdapter == null) {
            Log.e("DEBUG", "Exercise adapter is null! Cannot retrieve logs.");
            return;
        }

        List<WorkoutExerciseLog> exerciseLogs = exerciseAdapter.getWorkoutLogs();
        if (exerciseLogs == null || exerciseLogs.isEmpty()) {
            Log.e("DEBUG", "No exercise logs to save!");
            Toast.makeText(this, "No workout data to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create workout session object
        WorkoutSession workoutSession = new WorkoutSession(
                SessionManager.getLoggedInUserID(this), // Fetch userID from session
                getCurrentDate(), // Get today's date
                workoutPlan.getWorkouts().get(workoutPlan.getCurrentDayIndex()).getWorkoutName(),
                exerciseLogs
        );

        // Log the entire request before sending it
        Log.d("DEBUG", "WorkoutSession payload: " + ApiClient.getGson().toJson(workoutSession));

        // Send workout session data to backend
        apiService.saveWorkoutSession(workoutSession).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d("DEBUG", "Workout session saved successfully!");

                    // Get the next workout day from response
                    int nextDayIndex = response.body().getNextDayIndex();
                    workoutPlan.setCurrentDayIndex(nextDayIndex);

                    // Fetch next workout plan separately to avoid duplicate calls
                    fetchWorkoutPlan();
                } else {
                    Log.e("API_ERROR", "Failed to save workout session");
                    Toast.makeText(StartWorkoutActivity.this, "Error saving workout.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Workout session request failed: " + t.getMessage());
                Toast.makeText(StartWorkoutActivity.this, "Network error while saving workout.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper function to get current date in YYYY-MM-DD format
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }


    // Sets up bottom navigation and handles switching between tabs
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_workouts) {
                return true; // Stay on Workouts
            } else if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            } else if (id == R.id.nav_nutrition) {
                startActivity(new Intent(this, NutritionHomeActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // Set default selected tab to Workouts
        bottomNavigationView.setSelectedItemId(R.id.nav_workouts);
    }
}