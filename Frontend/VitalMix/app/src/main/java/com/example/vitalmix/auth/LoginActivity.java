package com.example.vitalmix.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.models.User;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.ui.DashboardActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEt, passwordEt;
    private Button loginBtn, registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Set the layout

        // Initialize UI xml components
        emailEt = findViewById(R.id.email_et);
        passwordEt = findViewById(R.id.password_et);
        loginBtn = findViewById(R.id.login_btn);
        registerBtn = findViewById(R.id.register_btn);

        // Set click listener for login button
        loginBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a User object with login credentials
            User user = new User();
            user.setEmail(email);
            user.setPassword(password);

            // Call the login endpoint via Retrofit
            loginUser(user);
        });

        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // Function to handle user login via API
    private void loginUser(User user) {
        ApiClient.getApiService().loginUser(user).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String redirectTo = response.body().getRedirectTo(); // Get redirection target
                    User loggedInUser = response.body().getUser(); // Get full user object

                    // Save userId & email in SessionManager
                    Log.d("LOGIN_DEBUG", "UserID received in login response: " + loggedInUser.getUserID());
                    SessionManager.saveLoggedInUserID(LoginActivity.this, loggedInUser.getUserID());
                    SessionManager.saveLoggedInUserEmail(LoginActivity.this, loggedInUser.getEmail());
                    SessionManager.saveLoggedInUserFirstName(LoginActivity.this, loggedInUser.getFirstName());

                    // Redirect based on questionnaire completion
                    if ("Questionnaire".equals(redirectTo)) {
                        Intent intent = new Intent(LoginActivity.this, QuestionnaireActivity.class);
                        startActivity(intent);
                    } else if ("Dashboard".equals(redirectTo)) {
                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(intent);
                    }

                    finish(); // Close activity
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}