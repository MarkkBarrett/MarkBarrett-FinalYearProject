package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vitalmix.R;
import com.example.vitalmix.adapters.WorkoutHistoryAdapter;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.models.WorkoutSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewWorkoutHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private WorkoutHistoryAdapter historyAdapter;
    private ApiService apiService;
    private String userId;
    private TextView titleTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_workout_history);

        // Initialize API
        apiService = ApiClient.getApiService();

        // Initialize views
        historyRecyclerView = findViewById(R.id.workout_history_rv);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        titleTv = findViewById(R.id.history_title_tv);

        // Get user ID from session
        userId = SessionManager.getLoggedInUserID(this);

        // Set title
        String firstName = SessionManager.getLoggedInUserFirstName(this);
        titleTv.setText(firstName + "'s Workout History");

        // Load workout history
        fetchWorkoutHistory();

        // Bottom navigation
        setupBottomNavigation();
    }

    // Fetches user's full workout history
    private void fetchWorkoutHistory() {
        apiService.getWorkoutHistory(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<WorkoutSession> sessions = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()),
                            ApiClient.getWorkoutSessionListType()
                    );

                    // Attach adapter
                    historyAdapter = new WorkoutHistoryAdapter(ViewWorkoutHistoryActivity.this, sessions);
                    historyRecyclerView.setAdapter(historyAdapter);

                    Log.d("workout_debug", "Workout history loaded: " + sessions.size() + " sessions");
                } else {
                    Toast.makeText(ViewWorkoutHistoryActivity.this, "Failed to load workout history", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", "Workout history fetch failed");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(ViewWorkoutHistoryActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", "Workout history fetch failed: " + t.getMessage());
            }
        });
    }

    // Setup bottom navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_workouts) {
                startActivity(new Intent(this, ChooseWorkoutActivity.class));
                return true;
            } else if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        //bottomNavigationView.setSelectedItemId(R.id.nav_workouts);
    }
}