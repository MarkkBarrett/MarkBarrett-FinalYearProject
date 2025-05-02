package com.example.vitalmix.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.vitalmix.R;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.api.ApiService;
import com.example.vitalmix.models.Exercise;
import com.example.vitalmix.models.WorkoutExerciseLog;
import com.example.vitalmix.models.WorkoutSession;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewProgressActivity extends AppCompatActivity {

    // chart: sessions per week
    private BarChart sessionFrequencyChart;
    // chart: weight over time
    private LineChart workoutProgressChart;
    private Spinner exerciseSpinnerHistory;
    private TextView noHistoryText;
    private Button startWorkoutBtn;
    private Button historyBtn;

    // hold all sessions and exercise mappings
    private List<WorkoutSession> workoutSessions = new ArrayList<>();
    private List<String> exerciseIds = new ArrayList<>();
    private Map<String, String> idToNameMap = new HashMap<>();

    // date formats
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_progress);

        // bind views
        sessionFrequencyChart = findViewById(R.id.sessions_week_chart);
        workoutProgressChart = findViewById(R.id.weight_progress_chart);
        exerciseSpinnerHistory = findViewById(R.id.exercise_spinner_history);
        startWorkoutBtn = findViewById(R.id.start_workout_btn);
        historyBtn = findViewById(R.id.history_btn);

        // set no data message
        workoutProgressChart.setNoDataText("No history yet – start your first workout!");
        workoutProgressChart.setNoDataTextColor(ContextCompat.getColor(this, R.color.green));
        workoutProgressChart.setNoDataTextTypeface(Typeface.DEFAULT_BOLD);

        setupBottomNavigation();

        // wire up buttons
        startWorkoutBtn.setOnClickListener(v -> onStartWorkout());
        historyBtn.setOnClickListener(v -> onViewHistory());

        // load data
        loadSessionData();
        loadExercises();
    }

    // 1) Sessions last 7 days – always show (empty = no bars)
    private void loadSessionData() {
        String userId = SessionManager.getLoggedInUserID(this);
        ApiService api = ApiClient.getApiService();
        api.getWorkoutHistory(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> resp) {
                float[] counts = new float[7]; // Mon=0 ... Sun=6

                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()
                        && resp.body().getData() != null) {

                    // parse sessions
                    List<WorkoutSession> sessions = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(resp.body().getData()), ApiClient.getWorkoutSessionListType());

                    // today and 6 days ago
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    Date today = cal.getTime();
                    cal.add(Calendar.DATE, -6);
                    Date weekAgo = cal.getTime();

                    for (WorkoutSession s : sessions) {
                        try {
                            Date d = isoFormat.parse(s.getSessionDate());
                            if (d != null && !d.before(weekAgo) && !d.after(today)) {
                                // map Mon=2→idx0, Sat=7→idx5, Sun=1→idx6
                                cal.setTime(d);
                                int dow = cal.get(Calendar.DAY_OF_WEEK);
                                int idx = (dow + 5) % 7;
                                counts[idx] += 1;
                            }
                        } catch (ParseException ignored) {
                        }
                    }
                }

                // build bar entries
                List<BarEntry> barEntries = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    barEntries.add(new BarEntry(i, counts[i]));
                }

                BarDataSet barDs = new BarDataSet(barEntries, "Sessions");
                barDs.setColor(getResources().getColor(R.color.green));
                BarData barData = new BarData(barDs);
                barData.setBarWidth(0.5f);

                sessionFrequencyChart.setData(barData);
                XAxis x2 = sessionFrequencyChart.getXAxis();
                x2.setGranularity(1f);
                x2.setPosition(XAxis.XAxisPosition.BOTTOM);
                x2.setValueFormatter(
                        new IndexAxisValueFormatter(Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")));
                sessionFrequencyChart.getAxisRight().setEnabled(false);
                sessionFrequencyChart.getDescription().setEnabled(false);
                sessionFrequencyChart.invalidate();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // leave chart empty
                sessionFrequencyChart.invalidate();
            }
        });
    }

    // Weight progress, populate spinner then chart
    private void loadExercises() {
        String userId = SessionManager.getLoggedInUserID(this);
        ApiService api = ApiClient.getApiService();

        api.getWorkoutHistory(userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()
                        && resp.body().getData() != null) {

                    // parse all sessions once
                    workoutSessions = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(resp.body().getData()),
                            ApiClient.getWorkoutSessionListType());

                    // collect unique exercise IDs
                    Set<String> idSet = new HashSet<>();
                    for (WorkoutSession s : workoutSessions) {
                        for (WorkoutExerciseLog log : s.getExerciseLogs()) {
                            idSet.add(log.getExerciseId());
                        }
                    }
                    exerciseIds = new ArrayList<>(idSet);

                    if (exerciseIds.isEmpty()) {
                        // no exercises, show message
                        exerciseSpinnerHistory.setVisibility(View.GONE);
                        workoutProgressChart.setVisibility(View.GONE);
                        noHistoryText.setVisibility(View.VISIBLE);
                        return;
                    }

                    // fetch names for those IDs
                    api.getExercisesByIds(exerciseIds).enqueue(new Callback<ApiResponse>() {
                        @Override
                        public void onResponse(Call<ApiResponse> c2, Response<ApiResponse> r2) {
                            List<String> names = new ArrayList<>();
                            if (r2.isSuccessful() && r2.body() != null && r2.body().isSuccess()) {
                                List<Exercise> exList = ApiClient.getGson().fromJson(
                                        ApiClient.getGson().toJson(r2.body().getData()),
                                        ApiClient.getExerciseListType());
                                for (Exercise ex : exList) {
                                    idToNameMap.put(ex.getId(), ex.getName());
                                }
                            }
                            // keep original order
                            for (String id : exerciseIds) {
                                names.add(idToNameMap.getOrDefault(id, id));
                            }

                            // populate spinner
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    ViewProgressActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    names);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            exerciseSpinnerHistory.setAdapter(adapter);

                            // load initial chart
                            exerciseSpinnerHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long itemId) {
                                    loadWeightData(exerciseIds.get(position));
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });
                            exerciseSpinnerHistory.setVisibility(View.VISIBLE);
                            loadWeightData(exerciseIds.get(0));
                        }

                        @Override
                        public void onFailure(Call<ApiResponse> c2, Throwable t2) {
                            // fallback, show spinner IDs only
                            List<String> names = new ArrayList<>(exerciseIds);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    ViewProgressActivity.this,
                                    android.R.layout.simple_spinner_item,
                                    names);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            exerciseSpinnerHistory.setAdapter(adapter);

                            exerciseSpinnerHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long itemId) {
                                    loadWeightData(exerciseIds.get(position));
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });

                            exerciseSpinnerHistory.setVisibility(View.VISIBLE);
                            loadWeightData(exerciseIds.get(0));
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // network failure, hide spinner/chart, show message
                exerciseSpinnerHistory.setVisibility(View.GONE);
                workoutProgressChart.setVisibility(View.GONE);
                noHistoryText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadWeightData(String exerciseId) {
        List<Entry> weightEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // collect weights for this exercise, sorted by date
        Collections.sort(workoutSessions, (s1, s2) -> {
            try {
                Date d1 = isoFormat.parse(s1.getSessionDate());
                Date d2 = isoFormat.parse(s2.getSessionDate());
                return d1.compareTo(d2);
            } catch (ParseException e) {
                return 0;
            }
        });

        int index = 0;
        for (WorkoutSession s : workoutSessions) {
            for (WorkoutExerciseLog log : s.getExerciseLogs()) {
                if (exerciseId.equals(log.getExerciseId())) {
                    weightEntries.add(new Entry(index, log.getLastUsedWeight()));
                    try {
                        Date d = isoFormat.parse(s.getSessionDate());
                        labels.add(displayFormat.format(d));
                    } catch (ParseException e) {
                        labels.add(s.getSessionDate());
                    }
                    index++;
                }
            }
        }

        if (weightEntries.isEmpty()) {
            workoutProgressChart.clear();
            workoutProgressChart.invalidate();
        } else {
            // hide the no data message and draw your line
            LineDataSet weightDs = new LineDataSet(weightEntries, "Weight Progress");
            weightDs.setColor(getResources().getColor(R.color.green));
            weightDs.setCircleColor(getResources().getColor(R.color.green));

            workoutProgressChart.setData(new LineData(weightDs));
            XAxis x1 = workoutProgressChart.getXAxis();
            x1.setGranularity(1f);
            x1.setValueFormatter(new IndexAxisValueFormatter(labels));
            workoutProgressChart.getDescription().setText("Weight over Sessions");
            workoutProgressChart.invalidate();
        }
    }

    // stub: navigate to StartWorkoutActivity
    private void onStartWorkout() {
        startActivity(new Intent(this, StartWorkoutActivity.class));
    }

    // stub: navigate to WorkoutHistoryActivity
    private void onViewHistory() {
        startActivity(new Intent(this, ViewWorkoutHistoryActivity.class));
    }

    // Setup bottom navigation
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // start dashboard
                // fresh
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_workouts) {
                startActivity(new Intent(this, ChooseWorkoutActivity.class));
                return true;
            } else if (id == R.id.nav_form) {
                startActivity(new Intent(this, FormCheckerActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // bottomNavigationView.setSelectedItemId(R.id.nav_workouts);
    }
}