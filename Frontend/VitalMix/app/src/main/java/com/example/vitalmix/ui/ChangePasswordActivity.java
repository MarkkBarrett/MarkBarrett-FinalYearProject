package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.models.ChangePassword;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentEt, newEt, confirmEt;
    private Button changeBtn;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize inputs
        currentEt = findViewById(R.id.current_password_et);
        newEt = findViewById(R.id.new_password_et);
        confirmEt = findViewById(R.id.confirm_password_et);
        changeBtn = findViewById(R.id.change_password_btn);
        apiService = ApiClient.getApiService();

        changeBtn.setOnClickListener(v -> handleChangePassword());

        setupBottomNavigation();
    }

    private void handleChangePassword() {
        String userId = SessionManager.getLoggedInUserID(this);
        String current = currentEt.getText().toString().trim();
        String newer = newEt.getText().toString().trim();
        String confirm = confirmEt.getText().toString().trim();

        if (current.isEmpty() || newer.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newer.equals(confirm)) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        ChangePassword request = new ChangePassword(userId, current, newer);
        apiService.changePassword(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                Log.d("PROFILE_DEBUG", "Response: " + ApiClient.getGson().toJson(response.body()));
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to profile
                } else {
                    String msg = (response.body() != null) ? response.body().getMessage() : "Error changing password";
                    Toast.makeText(ChangePasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(ChangePasswordActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ChangePassword", "API call failed", t);
            }
        });
    }

    // Bottom navigation logic
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
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
        //bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }
}