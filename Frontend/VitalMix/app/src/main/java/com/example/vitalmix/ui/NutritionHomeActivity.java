package com.example.vitalmix.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NutritionHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_home);

        setupBottomNavigation(); // Initialize bottom navigation
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
            } else if (id == R.id.nav_nutrition) {
                return true; // Stay on Nutrition
            } else if (id == R.id.nav_progress) {
                startActivity(new Intent(this, StartWorkoutActivity.class)); //ProgressActivity
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // Set the selected item to Nutrition
        bottomNavigationView.setSelectedItemId(R.id.nav_nutrition);
    }
}
