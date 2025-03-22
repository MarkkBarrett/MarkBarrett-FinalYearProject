package com.example.vitalmix.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vitalmix.R;
import com.example.vitalmix.api.ApiClientFastAPI;
import com.example.vitalmix.api.ApiResponseFastAPI;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.utils.FileUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import java.io.File;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FormCheckerActivity extends AppCompatActivity {

    private static final int VIDEO_PICK_REQUEST = 1;
    private Uri videoUri;
    private Spinner exerciseSpinner;
    private TextView uploadText, scoreText, feedbackText;
    private Button calculateScoreBtn;
    private VideoView resultVideoView;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_checker);

        // Initialize UI components
        exerciseSpinner = findViewById(R.id.exercise_spinner);
        uploadText = findViewById(R.id.upload_text);
        scoreText = findViewById(R.id.score_tv);
        feedbackText = findViewById(R.id.feedback_tv);
        calculateScoreBtn = findViewById(R.id.calculate_score_btn);
        RelativeLayout uploadContainer = findViewById(R.id.upload_container);
        resultVideoView = findViewById(R.id.processed_video_view);
        resultVideoView.setMediaController(new MediaController(this)); // Playback controls

        // Retrofit service
        apiService = ApiClientFastAPI.getApiService();

        // Set video picker
        uploadContainer.setOnClickListener(v -> selectVideo());

        // Set API trigger
        calculateScoreBtn.setOnClickListener(v -> {
            if (videoUri != null) {
                uploadVideo();
            } else {
                Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
            }
        });

        setupBottomNavigation();

    }

    // Launch picker
    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_PICK_REQUEST);
    }

    // Handle selected video
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            videoUri = data.getData();
            uploadText.setText("Video Selected");
        }
    }

    // Upload video and exercise name to FastAPI
    private void uploadVideo() {
        File videoFile = new File(FileUtils.getPath(this, videoUri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("file", videoFile.getName(), requestFile);

        String selectedExercise = exerciseSpinner.getSelectedItem().toString();
        RequestBody exerciseBody = RequestBody.create(MediaType.parse("text/plain"), selectedExercise);

        Call<ApiResponseFastAPI> call = apiService.uploadFormCheck(videoPart, exerciseBody);
        call.enqueue(new Callback<ApiResponseFastAPI>() {
            @Override
            public void onResponse(Call<ApiResponseFastAPI> call, Response<ApiResponseFastAPI> response) {
                Log.d("FormCheck", "Raw response: " + response.toString());

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("FormCheck", "Full API Response: " + new Gson().toJson(response.body()));

                    Map<String, Object> data = response.body().getData();
                    if (data != null) {
                        double accuracy = (double) data.get("accuracy");
                        String feedback = data.get("feedback").toString();
                        String videoUrl = response.body().getVideoUrl();

                        scoreText.setText("Score: " + accuracy + "%");
                        feedbackText.setText("Feedback: " + feedback);

                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            playProcessedVideo(videoUrl);
                        } else {
                            Log.e("FormCheck", "Video URL is null or empty!");
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponseFastAPI> call, Throwable t) {
                Log.e("FormCheck", "API call failed", t);
            }
        });
    }

    // Stream the processed video directly
    private void playProcessedVideo(String videoUrl) {
        try {
            Log.d("FormCheck", "Streaming processed video from: " + videoUrl);
            resultVideoView.setVideoURI(Uri.parse(videoUrl));
            resultVideoView.setVisibility(View.VISIBLE);
            resultVideoView.start();
        } catch (Exception e) {
            Log.e("FormCheck", "Failed to play video", e);
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
        }
    }

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
                return true; // Stay on form checker
            } else if (id == R.id.nav_nutrition) {
                startActivity(new Intent(this, NutritionHomeActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_form); // highlight form tab
    }
}
