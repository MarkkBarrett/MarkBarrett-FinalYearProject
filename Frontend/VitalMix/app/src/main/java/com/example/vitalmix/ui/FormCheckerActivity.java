package com.example.vitalmix.ui;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
    private Uri videoUri; // Stores the selected video URI
    private Spinner exerciseSpinner;
    private TextView uploadText, scoreText, feedbackText;
    private Button calculateScoreBtn;
    private VideoView resultVideoView; // VideoView for displaying processed video
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
        resultVideoView.setMediaController(new MediaController(this)); // Enable playback controls

        // Initialize Retrofit API service
        apiService = ApiClientFastAPI.getApiService();

        // Set up video selection
        uploadContainer.setOnClickListener(v -> selectVideo());

        // Set up button to send request
        calculateScoreBtn.setOnClickListener(v -> {
            if (videoUri != null) {
                uploadVideo();
            } else {
                Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Opens file picker to select a video
    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            videoUri = data.getData();
            uploadText.setText("Video Selected");
        }
    }

    // Sends the selected video & exercise metadata to FastAPI
    private void uploadVideo() {
        File videoFile = new File(FileUtils.getPath(this, videoUri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("video/mp4"), videoFile);
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("file", videoFile.getName(), requestFile);

        // Get selected exercise
        String selectedExercise = exerciseSpinner.getSelectedItem().toString();
        RequestBody exerciseBody = RequestBody.create(MediaType.parse("text/plain"), selectedExercise);

        // Make API call
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

                        // Play processed video
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

    // Play the processed video
    private void playProcessedVideo(String videoUrl) {
        try {
            Log.d("FormCheck", "Attempting to download processed video: " + videoUrl);

            // Use DownloadManager to download the video
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VitalMixProcessed.mp4"); // Save in Downloads
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            // Actively poll for download completion instead of relying only on broadcast receiver
            new Thread(() -> {
                boolean isDownloaded = false;
                File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VitalMixProcessed.mp4");

                while (!isDownloaded) {
                    if (downloadedFile.exists()) {
                        isDownloaded = true;
                        Log.d("DownloadCheck", "Download confirmed. File exists at: " + downloadedFile.getAbsolutePath());

                        // Force a media scan to register the file
                        MediaScannerConnection.scanFile(this, new String[]{downloadedFile.getAbsolutePath()}, null,
                                (path, uri) -> {
                                    Log.d("MediaScan", "Scan completed for: " + path);

                                    runOnUiThread(() -> {
                                        // Play downloaded video after ensuring it's registered
                                        Uri downloadedFileUri = Uri.fromFile(downloadedFile);
                                        resultVideoView.setVideoURI(downloadedFileUri);
                                        resultVideoView.start();
                                        resultVideoView.setVisibility(View.VISIBLE);
                                    });
                                });
                    } else {
                        Log.d("DownloadCheck", "Waiting for file to appear...");
                        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e("FormCheck", "Error downloading video", e);
            Toast.makeText(this, "Error downloading video", Toast.LENGTH_SHORT).show();
        }
    }
}