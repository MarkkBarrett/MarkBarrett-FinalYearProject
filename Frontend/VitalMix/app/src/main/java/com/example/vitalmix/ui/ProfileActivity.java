package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBottomNavigation(); // Initialize the bottom navigation
    }

    // Method to set up bottom navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Handle navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_workouts) {
                startActivity(new Intent(this, StartWorkoutActivity.class));
                return true;
            } else if (id == R.id.nav_nutrition) {
                startActivity(new Intent(this, NutritionHomeActivity.class));
                return true;
            } else if (id == R.id.nav_progress) {
                startActivity(new Intent(this, DashboardActivity.class)); //ProgressActivity
                return true;
            } else if (id == R.id.nav_profile) {
                return true; // Stay on the profile
            }
            return false;
        });

        // Set the selected item to profile
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }
}