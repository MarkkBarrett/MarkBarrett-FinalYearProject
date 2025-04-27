package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vitalmix.R;
import com.example.vitalmix.adapters.DefaultWorkoutPlanAdapter;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.models.DefaultWorkoutPlan;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChooseWorkoutActivity extends AppCompatActivity {

    private RecyclerView plansRecyclerView;
    private DefaultWorkoutPlanAdapter planAdapter;
    private ApiService apiService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_workout);

        // Initialize API service
        apiService = ApiClient.getApiService();

        // Get userid from session
        userId = SessionManager.getLoggedInUserID(this);

        // Set up RecyclerView
        plansRecyclerView = findViewById(R.id.workout_plans_rv);
        plansRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load all workout plans initially
        loadAllPlansThenFetchRecommendation();

        // Setup button click listeners
        setupButtonListeners();

        // Setup bottom nav bar
        setupBottomNavigation();
    }

    // Loads all default workout plans, then fetches and highlights the recommendation
    private void loadAllPlansThenFetchRecommendation() {
        Log.d("workout_debug", "Starting to load all default workout plans");

        apiService.getDefaultWorkoutPlans().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                Log.d("workout_debug", "Default plans response success: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d("workout_debug", "Parsing default plans from API response");

                    List<DefaultWorkoutPlan> plans = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()),
                            ApiClient.getDefaultWorkoutPlanListType()
                    );

                    Log.d("workout_debug", "Parsed plans count: " + plans.size());

                    // Set adapter with all plans
                    planAdapter = new DefaultWorkoutPlanAdapter(ChooseWorkoutActivity.this, plans);
                    plansRecyclerView.setAdapter(planAdapter);

                    Log.d("workout_debug", "Adapter set. Now fetching recommendation.");
                    fetchAndHighlightRecommendation();

                } else {
                    Log.e("workout_debug", "Failed to load plans: " + response.code());
                    Toast.makeText(ChooseWorkoutActivity.this, "Failed to load plans", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("workout_debug", "Error fetching default plans: " + t.getMessage());
                Toast.makeText(ChooseWorkoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetches recommended plan and set adapter to highlight it
    private void fetchAndHighlightRecommendation() {
        apiService.getRecommendedPlan(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                Log.d("DEBUG", "Recommendation Response success: " + response.isSuccessful());
                Log.d("DEBUG", "Body is null: " + (response.body() == null));

                if (response.body() != null && response.body().isSuccess()) {
                    DefaultWorkoutPlan recommendedPlan = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()),
                            DefaultWorkoutPlan.class
                    );

                    Log.d("DEBUG", "Recommended plan: " + recommendedPlan.getPlanName() + ", ID: " + recommendedPlan.getId());

                    // Highlight the recommended plan in the adapter
                    planAdapter.setRecommendedPlanId(recommendedPlan.getId());
                } else {
                    Log.w("DEBUG", "No recommendation found. Showing all plans without highlight.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e("API_ERROR", "Recommendation fetch failed: " + t.getMessage());
            }
        });
    }

    // Set up buttons
    private void setupButtonListeners() {
        findViewById(R.id.start_workout_btn).setOnClickListener(v ->
                startActivity(new Intent(this, StartWorkoutActivity.class)));

        findViewById(R.id.create_plan_btn).setOnClickListener(v ->
                Toast.makeText(this, "Create Workout Plan coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.view_progress_btn).setOnClickListener(v ->
                Toast.makeText(this, "View Progress coming soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.workout_history_btn).setOnClickListener(v ->
                startActivity(new Intent(this, ViewWorkoutHistoryActivity.class)));
    }

    // Handles tab switching on the bottom navigation bar
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_workouts) {
                return true; // Stay on this page
            } else if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_workouts); // highlight Workouts tab
    }
}