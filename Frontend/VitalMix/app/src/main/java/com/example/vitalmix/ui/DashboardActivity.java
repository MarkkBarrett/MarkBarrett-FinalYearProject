package com.example.vitalmix.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.vitalmix.R;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.models.WorkoutSession;
import com.example.vitalmix.models.WorkoutExerciseLog;
import com.example.vitalmix.models.Exercise;
import com.example.vitalmix.models.FormResult;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private TextView welcomeTv;
    private TextView lastWorkoutTv;
    private TextView accuracyTv; // form checker accuracy
    private PieChart formAccuracyPercent;
    private CardView lastWorkoutCard;
    private TextView cardWorkoutName, cardWorkoutDate;
    private LinearLayout cardExercisesContainer;

    // parse ISO dates from backend
    private static final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault());
    // show dates like Apr 26, 2025
    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // bind all views
        initializeViews();

        // personalise greeting
        updateWelcomeText();

        // fetch and display last workout
        fetchAndDisplayLastWorkout();

        // setup bottom nav
        setupBottomNavigation();

        // setup charts with sample data
        setupChart();

        findViewById(R.id.start_workout_btn)
                .setOnClickListener(v -> startActivity(new Intent(this, StartWorkoutActivity.class)));
    }

    // bind UI elements to fields
    private void initializeViews() {
        welcomeTv = findViewById(R.id.welcome_tv);
        lastWorkoutTv = findViewById(R.id.last_workout_tv);
        accuracyTv = findViewById(R.id.accuracy_tv);
        formAccuracyPercent = findViewById(R.id.form_accuracy_chart);
        lastWorkoutCard = findViewById(R.id.last_workout_card);
        cardWorkoutName = findViewById(R.id.card_workout_name);
        cardWorkoutDate = findViewById(R.id.card_workout_date);
        cardExercisesContainer = findViewById(R.id.card_exercises_container);
    }

    // set welcome text using name
    private void updateWelcomeText() {
        String firstName = SessionManager.getLoggedInUserFirstName(this);
        if (firstName != null && !firstName.isEmpty()) {
            welcomeTv.setText("Hi, " + firstName + "! Small steps today, big achievements tomorrow!");
        }
    }

    // call backend for workout history and show the latest one
    private void fetchAndDisplayLastWorkout() {
        String userId = SessionManager.getLoggedInUserID(this);
        ApiService api = ApiClient.getApiService();
        api.getWorkoutHistory(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()
                        && response.body().getData() != null) {

                    // parse raw data into List<WorkoutSession>
                    List<WorkoutSession> sessions = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()),
                            ApiClient.getWorkoutSessionListType());

                    // sort sessions by date
                    Collections.sort(sessions, (o1, o2) -> {
                        try {
                            Date d1 = isoFormat.parse(o1.getSessionDate());
                            Date d2 = isoFormat.parse(o2.getSessionDate());
                            return d1.compareTo(d2);
                        } catch (ParseException e) {
                            return 0;
                        }
                    });

                    // get the very last session
                    final WorkoutSession lastSession = sessions.get(sessions.size() - 1);

                    // format date
                    String dateStr;
                    try {
                        Date d = isoFormat.parse(lastSession.getSessionDate());
                        dateStr = displayFormat.format(d);
                    } catch (Exception e) {
                        dateStr = lastSession.getSessionDate();
                    }
                    lastWorkoutTv.setText(lastSession.getWorkoutName() + " on " + dateStr);

                    // fetch exercise names for that session
                    List<String> ids = new ArrayList<>();
                    for (WorkoutExerciseLog log : lastSession.getExerciseLogs()) {
                        ids.add(log.getExerciseId());
                    }
                    ApiClient.getApiService().getExercisesByIds(ids).enqueue(new Callback<ApiResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse> c2, @NonNull Response<ApiResponse> r2) {
                            Map<String, String> idToName = new HashMap<>();
                            if (r2.isSuccessful() && r2.body() != null && r2.body().isSuccess()) {
                                List<Exercise> exList = ApiClient.getGson().fromJson(
                                        ApiClient.getGson().toJson(r2.body().getData()),
                                        ApiClient.getExerciseListType());
                                for (Exercise ex : exList) {
                                    idToName.put(ex.getId(), ex.getName());
                                }
                            }
                            // display card with names
                            displayLastWorkoutCard(lastSession, idToName);
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse> c2, @NonNull Throwable t2) {
                            // fallback, show IDs if names fail
                            displayLastWorkoutCard(lastSession, null);
                        }
                    });

                } else {
                    lastWorkoutCard.setVisibility(View.VISIBLE);
                    cardWorkoutName.setText("Last workout!");
                    cardWorkoutDate.setText("Pick a workout plan and complete your first workout!");
                    cardExercisesContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                lastWorkoutTv.setText("Error loading workout");
            }
        });
    }

    private void setupChart() {
        formAccuracyPercent.setNoDataText("No form data to display yet"); // text if no results
        formAccuracyPercent.setNoDataTextColor(ContextCompat.getColor(this, R.color.green));
        formAccuracyPercent.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);

        String userId = SessionManager.getLoggedInUserID(this);
        ApiClient.getApiService().getFormResults(userId, "squat", 5).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.body() == null || !response.body().isSuccess()) {
                    formAccuracyPercent.clear();
                    formAccuracyPercent.invalidate();
                    return;
                }
                // parse results
                List<FormResult> results = ApiClient.getGson().fromJson(
                        ApiClient.getGson().toJson(response.body().getData()), new TypeToken<List<FormResult>>() {
                        }.getType());
                if (results.isEmpty()) {
                    formAccuracyPercent.clear();
                    formAccuracyPercent.invalidate();
                    return;
                }
                // show latest accuracy in the TextView
                float latest = (float) results.get(0).getAccuracy();
                accuracyTv.setText(String.format(Locale.getDefault(), "%.0f%%", latest));

                // average slice vs remainder
                float sum = 0f;
                for (FormResult r : results)
                    sum += r.getAccuracy();
                float avg = sum / results.size();

                // label slices
                List<PieEntry> slices = Arrays.asList(new PieEntry(avg, "Correct"),
                        new PieEntry(100f - avg, "Improvement"));

                // show the avg% as large white center text
                formAccuracyPercent.setCenterText(String.format(Locale.getDefault(), "%.0f%%", avg));
                formAccuracyPercent.setCenterTextSize(24f);
                formAccuracyPercent.setCenterTextColor(ContextCompat.getColor(DashboardActivity.this, R.color.white));

                // style & bind
                PieDataSet ds = new PieDataSet(slices, "");
                ds.setColors(ContextCompat.getColor(DashboardActivity.this, R.color.green),
                        ContextCompat.getColor(DashboardActivity.this, R.color.gray));
                ds.setSliceSpace(2f);
                formAccuracyPercent.setData(new PieData(ds));
                formAccuracyPercent.setDrawHoleEnabled(false);
                formAccuracyPercent.getLegend().setEnabled(false);
                formAccuracyPercent.getDescription().setEnabled(false);
                formAccuracyPercent.invalidate();

                // catch any tap anywhere on chart not working but not a priority
                formAccuracyPercent.setOnClickListener(v -> {
                    if (results.isEmpty())
                        return;

                    StringBuilder sb = new StringBuilder();
                    for (FormResult r : results) {
                        sb.append(String.format(Locale.getDefault(), "%.0f%% – %s\n\n", r.getAccuracy(),
                                TextUtils.join(", ", r.getFeedback())));
                    }

                    new AlertDialog.Builder(DashboardActivity.this).setTitle("Last Results")
                            .setMessage(sb.toString().trim()).setPositiveButton("OK", null).show();
                });
            }

            @Override
            public void onFailure(Call<ApiResponse> c, Throwable t) {
                formAccuracyPercent.clear();
                formAccuracyPercent.invalidate();
            }
        });
    }

    // show the card with workout name, date, and each exercise/weight/reps
    private void displayLastWorkoutCard(WorkoutSession session, @Nullable Map<String, String> idToNameMap) {
        lastWorkoutCard.setVisibility(View.VISIBLE);

        // name & date
        cardWorkoutName.setText(session.getWorkoutName());
        try {
            Date d = isoFormat.parse(session.getSessionDate());
            cardWorkoutDate.setText(displayFormat.format(d));
        } catch (Exception e) {
            cardWorkoutDate.setText(session.getSessionDate());
        }

        // clear old rows
        cardExercisesContainer.removeAllViews();

        // for each log, show either the name or Id
        for (WorkoutExerciseLog log : session.getExerciseLogs()) {
            String name = (idToNameMap != null && idToNameMap.containsKey(log.getExerciseId()))
                    ? idToNameMap.get(log.getExerciseId())
                    : log.getExerciseId(); // fallback

            String text = name + ": " + String.format(Locale.getDefault(), "%.0f", log.getLastUsedWeight()) + "kg × "
                    + log.getLastUsedReps();

            TextView tv = new TextView(this);
            tv.setText(text);
            tv.setTextSize(14f);
            tv.setPadding(0, 8, 0, 8);
            cardExercisesContainer.addView(tv);
        }
    }

    // bottom nav
    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard)
                return true;
            if (id == R.id.nav_workouts) {
                startActivity(new Intent(this, ChooseWorkoutActivity.class));
                return true;
            }
            if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
        nav.setSelectedItemId(R.id.nav_dashboard);
    }
}