package com.example.vitalmix.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vitalmix.R;
import com.example.vitalmix.models.User;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    // Declare UI elements for user input
    private EditText fnameET, lnameET, emailET, passwordET;
    private Button registerBtn, loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Set the layout

        // Initialize UI xml components
        fnameET = findViewById(R.id.fname_et);
        lnameET = findViewById(R.id.lname_et);
        emailET = findViewById(R.id.email_et);
        passwordET = findViewById(R.id.password_et);
        registerBtn = findViewById(R.id.register_btn);
        loginBtn = findViewById(R.id.login_btn);

        // Set an onClickListener for the register button
        registerBtn.setOnClickListener(v -> {
            // Get user input values
            String firstName = fnameET.getText().toString().trim();
            String lastName = lnameET.getText().toString().trim();
            String email = emailET.getText().toString().trim();
            String password = passwordET.getText().toString().trim();

            // Validate user input
            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a new User object with the input values
            User user = new User(firstName, lastName, email, password);

            // Call the register method to send data to the server
            registerUser(user);
        });

        loginBtn.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    // Method to handle user registration via API
    private void registerUser(User user) {
        //Call Api and handle responses
        ApiClient.getApiService().registerUser(user).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    // Successful registration
                    Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                    // Redirect user to LoginActivity
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish(); // Close activity
                } else {
                    // Unsuccessful registration
                    Toast.makeText(RegisterActivity.this, "Registration Failed. Try a different email.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Handle network errors
                Toast.makeText(RegisterActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}