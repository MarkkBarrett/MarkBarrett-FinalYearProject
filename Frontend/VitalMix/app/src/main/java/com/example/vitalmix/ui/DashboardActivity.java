package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.auth.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //set name
        //updateWelcomeText();

        setupBottomNavigation(); // Initialize the bottom navigation
    }

    // Method to set up bottom navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Handle navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true; // Stay on the dashboard
            } else if (id == R.id.nav_workouts) {
                startActivity(new Intent(this, ChooseWorkoutActivity.class));
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

        // Set the selected item to dashboard
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
    }
}