package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.auth.LoginActivity;
import com.example.vitalmix.auth.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTv, emailTv;
    private EditText heightTv, weightTv, ageTv;
    private Button updateProfileBtn, saveChangesBtn, deleteAccountBtn;
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
        updateProfileBtn = findViewById(R.id.update_profile_btn);
        saveChangesBtn = findViewById(R.id.save_changes_btn);
        deleteAccountBtn = findViewById(R.id.delete_account_btn);

        // Initialize API service
        apiService = ApiClient.getApiService();

        // Load and display user profile info
        fetchUserProfile();

        // Setup bottom navigation
        setupBottomNavigation();

        // Handle buttons
        updateProfile();
        saveChanges();
        changePassword();
        logout();
        deleteAccount();
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

    // Handle Update profile button, makes ET's editable and shows Save button
    private void updateProfile() {
        updateProfileBtn.setOnClickListener(v -> {
            // Enable fields
            ageTv.setEnabled(true);
            heightTv.setEnabled(true);
            weightTv.setEnabled(true);

            // Set visual style for editable fields
            int black = ContextCompat.getColor(this, R.color.black);

            ageTv.setBackgroundResource(R.drawable.border);
            heightTv.setBackgroundResource(R.drawable.border);
            weightTv.setBackgroundResource(R.drawable.border);

            ageTv.setTextColor(black);
            heightTv.setTextColor(black);
            weightTv.setTextColor(black);

            heightTv.setText(heightTv.getText().toString().replace(" cm", ""));
            weightTv.setText(weightTv.getText().toString().replace(" kg", ""));

            // Show Save button
            saveChangesBtn.setVisibility(View.VISIBLE);
        });
    }

    // Handles sending updated values to backend
    private void saveChanges() {
        saveChangesBtn.setOnClickListener(v -> {
            String userId = SessionManager.getLoggedInUserID(this);

            // Validate and parse inputs
            String ageStr = ageTv.getText().toString().trim();
            String heightStr = heightTv.getText().toString().trim();
            String weightStr = weightTv.getText().toString().trim();

            if (ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int age = Integer.parseInt(ageStr);
            int height = Integer.parseInt(heightStr);
            int weight = Integer.parseInt(weightStr);

            JsonObject request = new JsonObject();
            request.addProperty("_id", userId);
            request.addProperty("age", age);
            request.addProperty("height", height);
            request.addProperty("weight", weight);

            apiService.updateProfile(request).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        saveChangesBtn.setVisibility(View.GONE);

                        // Disable fields again
                        ageTv.setEnabled(false);
                        heightTv.setEnabled(false);
                        weightTv.setEnabled(false);

                        int green = ContextCompat.getColor(ProfileActivity.this, R.color.green);
                        int white = ContextCompat.getColor(ProfileActivity.this, R.color.white);

                        ageTv.setBackgroundColor(green);
                        heightTv.setBackgroundColor(green);
                        weightTv.setBackgroundColor(green);

                        ageTv.setTextColor(white);
                        heightTv.setTextColor(white);
                        weightTv.setTextColor(white);

                        heightTv.setText(height + " cm");
                        weightTv.setText(weight + " kg");
                    } else {
                        String msg = (response.body() != null) ? response.body().getMessage() : "Update failed";
                        Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProfileUpdate", "API call failed", t);
                }
            });
        });
    }

    // Handles Change Password button
    private void changePassword() {
        findViewById(R.id.change_password_btn).setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });
    }

    // Handles logout button
    private void logout() {
        findViewById(R.id.logout_btn).setOnClickListener(v -> {
            // Clear session data
            SessionManager.clearSession(this);

            // Redirect to login and clear activity stack
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // Handles Delete Account button with confirmation dialog
    private void deleteAccount() {
        deleteAccountBtn.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Are you sure you want to delete this account?")
                    .setMessage("This will permanently delete your account and all associated data. Do you want to continue?")
                    .setPositiveButton("Yes, delete", (dialog, which) -> {
                        String userId = SessionManager.getLoggedInUserID(this);

                        apiService.deleteAccount(userId).enqueue(new Callback<ApiResponse>() {
                            @Override
                            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                    Toast.makeText(ProfileActivity.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                    // Clear session and return to login
                                    SessionManager.clearSession(ProfileActivity.this);
                                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    String msg = (response.body() != null) ? response.body().getMessage() : "Deletion failed";
                                    Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse> call, Throwable t) {
                                Toast.makeText(ProfileActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("AccountDelete", "API call failed", t);
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
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
                startActivity(new Intent(this, ChooseWorkoutActivity.class));
                return true;
            } else if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }
}