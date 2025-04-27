package com.example.vitalmix.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vitalmix.R;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.models.Exercise;
import com.example.vitalmix.models.WorkoutSession;
import com.example.vitalmix.models.WorkoutExerciseLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Adapter to display past workout sessions
public class WorkoutHistoryAdapter extends RecyclerView.Adapter<WorkoutHistoryAdapter.SessionViewHolder> {

    private final Context context;
    private final List<WorkoutSession> workoutSessions;

    // Constructor
    public WorkoutHistoryAdapter(Context context, List<WorkoutSession> workoutSessions) {
        this.context = context;
        this.workoutSessions = workoutSessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.workout_history_row, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        WorkoutSession session = workoutSessions.get(position);

        // Set workout name and date
        holder.workoutNameTv.setText(session.getWorkoutName());
        // parse backend ISO date and format like Apr 26, 2025
        String rawDate = session.getSessionDate();
        String formattedDate;
        try {
            Date date = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()
            ).parse(rawDate);
            formattedDate = new SimpleDateFormat(
                    "MMM dd, yyyy", Locale.getDefault()
            ).format(date);
        } catch (Exception e) {
            formattedDate = rawDate;
        }
        holder.sessionDateTv.setText(formattedDate);

        // Handle row click
        holder.itemView.setOnClickListener(v -> {
            showWorkoutDetailsDialog(session);
        });
    }

    @Override
    public int getItemCount() {
        return workoutSessions.size();
    }

    // ViewHolder class
    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView workoutNameTv, sessionDateTv;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            workoutNameTv = itemView.findViewById(R.id.workout_name_tv);
            sessionDateTv = itemView.findViewById(R.id.session_date_tv);
        }
    }

    // Show a dialog with workout details (exercise names and last used weights)
    private void showWorkoutDetailsDialog(WorkoutSession session) {
        List<String> exerciseIds = new ArrayList<>();

        // Collect all exerciseIds
        if (session.getExerciseLogs() != null) {
            for (WorkoutExerciseLog log : session.getExerciseLogs()) {
                exerciseIds.add(log.getExerciseId());
            }
        }

        // Make API call to fetch exercise names
        ApiClient.getApiService().getExercisesByIds(exerciseIds).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Exercise> exercises = ApiClient.getGson().fromJson(
                            ApiClient.getGson().toJson(response.body().getData()),
                            ApiClient.getExerciseListType()
                    );

                    // Map exerciseId -> name
                    Map<String, String> idToNameMap = new HashMap<>();
                    for (Exercise ex : exercises) {
                        idToNameMap.put(ex.getId(), ex.getName());
                    }

                    // Build message
                    StringBuilder message = new StringBuilder();
                    for (WorkoutExerciseLog log : session.getExerciseLogs()) {
                        String exerciseName = idToNameMap.getOrDefault(log.getExerciseId(), "Unknown Exercise");
                        message.append("- ").append(exerciseName)
                                .append("\nLast Used Weight: ").append(log.getLastUsedWeight())
                                .append(" kg\n\n");
                    }

                    // Show dialog
                    new AlertDialog.Builder(context)
                            .setTitle(session.getWorkoutName())
                            .setMessage(message.toString())
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .show();

                } else {
                    // Handle error fallback
                    new AlertDialog.Builder(context)
                            .setTitle("Error")
                            .setMessage("Failed to load exercise names.")
                            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                new AlertDialog.Builder(context)
                        .setTitle("Network Error")
                        .setMessage("Could not fetch exercise names.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });
    }
}