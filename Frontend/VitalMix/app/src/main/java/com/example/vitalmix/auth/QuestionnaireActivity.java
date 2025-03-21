package com.example.vitalmix.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.models.User;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.models.Questionnaire;
import com.example.vitalmix.ui.DashboardActivity;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuestionnaireActivity extends AppCompatActivity {

    private EditText genderET, ageET, heightET, weightET, dietInfoET;
    private Spinner fitnessGoalsSpinner, activityLevelSpinner;
    private Button saveBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        // Initialize all views
        genderET = findViewById(R.id.gender_et);
        ageET = findViewById(R.id.age_et);
        heightET = findViewById(R.id.height_et);
        weightET = findViewById(R.id.weight_et);
        dietInfoET = findViewById(R.id.diet_info_et);
        fitnessGoalsSpinner = findViewById(R.id.fitness_goals_spinner);
        activityLevelSpinner = findViewById(R.id.activity_level_spinner);
        saveBtn = findViewById(R.id.save_btn);
        progressBar = findViewById(R.id.progress_bar);

        // Handle save button click
        saveBtn.setOnClickListener(v -> saveQuestionnaire());
    }

    private void saveQuestionnaire() {
        // Show the progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Get inputs
        String gender = genderET.getText().toString().trim();
        String ageStr = ageET.getText().toString().trim();
        String heightStr = heightET.getText().toString().trim();
        String weightStr = weightET.getText().toString().trim();
        String fitnessGoals = fitnessGoalsSpinner.getSelectedItem().toString();
        String activityLevel = activityLevelSpinner.getSelectedItem().toString();
        String dietaryInfo = dietInfoET.getText().toString().trim();

        // Validate inputs
        if (gender.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty() || fitnessGoals.isEmpty() || activityLevel.isEmpty() || dietaryInfo.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        int age = Integer.parseInt(ageStr);
        int height = Integer.parseInt(heightStr);
        int weight = Integer.parseInt(weightStr);

        // Create the questionnaire object
        Questionnaire questionnaire = new Questionnaire(gender, age, height, weight, fitnessGoals, activityLevel, dietaryInfo);

        // Create the user object
        String loggedInEmail = SessionManager.getLoggedInUserEmail(this); // get email from session manager
        User user = new User();
        user.setEmail(loggedInEmail);
        user.setQuestionnaire(questionnaire);
        //already being set in backend
        //user.setHasCompletedQuestionnaire(true);

        //Checking questionnaire format for mismatches
        Log.d("QuestionnaireSubmission", new Gson().toJson(user));
        // Call the API to save the questionnaire
        ApiClient.getApiService().saveQuestionnaire(user).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    // Navigate to Dashboard
                    Toast.makeText(QuestionnaireActivity.this, "Questionnaire saved successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(QuestionnaireActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(QuestionnaireActivity.this, "Failed to save questionnaire", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(QuestionnaireActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}