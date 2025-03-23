package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.auth.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTv, emailTv, heightTv, weightTv, ageTv;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize UI elements
        nameTv = findViewById(R.id.name_tv);
        emailTv = findViewById(R.id.email_tv);
        heightTv = findViewById(R.id.height_tv);
        weightTv = findViewById(R.id.weight_tv);
        ageTv = findViewById(R.id.age_tv);

        // Initialize API service
        apiService = ApiClient.getApiService();

        // Load and display user profile info
        fetchUserProfile();

        // Setup bottom navigation
        setupBottomNavigation();
    }

    // Fetch user profile data from backend
    private void fetchUserProfile() {
        String userId = SessionManager.getLoggedInUserID(this); // Get userId from shared preferences

        // get user details
        apiService.getUserProfile(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Parse data field using Gson
                    JsonObject data = ApiClient.getGson().toJsonTree(response.body().getData()).getAsJsonObject();

                    String name = data.has("name") ? data.get("name").getAsString() : "";
                    String email = data.has("email") ? data.get("email").getAsString() : "";
                    String height = data.has("height") ? data.get("height").getAsInt() + " cm" : "";
                    String weight = data.has("weight") ? data.get("weight").getAsInt() + " kg" : "";
                    String age = data.has("age") ? String.valueOf(data.get("age").getAsInt()) : "";

                    // Update UI with user details
                    nameTv.setText(name);
                    emailTv.setText(email);
                    heightTv.setText(height);
                    weightTv.setText(weight);
                    ageTv.setText(age);
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    Log.e("ProfileAPI", "Response error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ProfileAPI", "API failure: ", t);
            }
        });
    }

    // Method to set up bottom navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_workouts) {
                startActivity(new Intent(this, StartWorkoutActivity.class));
                return true;
            } else if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            } else if (id == R.id.nav_nutrition) {
                startActivity(new Intent(this, NutritionHomeActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }
}