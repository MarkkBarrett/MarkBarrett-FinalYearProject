package com.example.vitalmix.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vitalmix.R;
import com.example.vitalmix.models.Exercise;
import com.example.vitalmix.models.SetLog;
import com.example.vitalmix.models.WorkoutExerciseLog;

import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    private final List<Exercise> exercises; // List of exercises
    private final Context context;
    private static final int DEFAULT_SETS = 4; // Hardcoded default sets
    private RecyclerView recyclerView;

    // Constructor
    public ExerciseAdapter(Context context, List<Exercise> exercises) {
        this.context = context;
        this.exercises = exercises;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView; // Store reference to RecyclerView
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.exercise_rv_row, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exercises.get(position); // Get current exercise

        // Set exercise name
        holder.exerciseTitle.setText(exercise.getName());

        // Clear any previous views to avoid duplication
        holder.setsRepsLayout.removeAllViews();
        holder.setRepsInputs.clear();
        holder.setWeightInputs.clear();

        // Dynamically add set rows
        for (int i = 0; i < DEFAULT_SETS; i++) {
            View setRow = LayoutInflater.from(context).inflate(R.layout.row_set, holder.setsRepsLayout, false);

            // Get references to EditText fields
            TextView setNumber = setRow.findViewById(R.id.set_number_tv);
            EditText repsInput = setRow.findViewById(R.id.reps_et);
            EditText weightInput = setRow.findViewById(R.id.weight_et);

            // Label the set number
            setNumber.setText("Set " + (i + 1));

            // Store references for later retrieval
            holder.setRepsInputs.add(repsInput);
            holder.setWeightInputs.add(weightInput);

            // Add the dynamically created row to the layout
            holder.setsRepsLayout.addView(setRow);
        }

        // Handle clicking on an exercise row
        holder.itemView.setOnClickListener(v -> showExerciseDetailsDialog(exercise));
    }

    // Retrieve workout logs from adapter
    public List<WorkoutExerciseLog> getWorkoutLogs() {
        if (exercises == null || exercises.isEmpty()) {
            Log.e("DEBUG", "Exercise list in adapter is null or empty!");
            return new ArrayList<>();
        }

        List<WorkoutExerciseLog> logs = new ArrayList<>();

        for (int position = 0; position < exercises.size(); position++) {
            Exercise exercise = exercises.get(position);
            Log.d("DEBUG", "Processing exercise: " + exercise.getName());

            // Retrieve the ViewHolder for this exercise
            RecyclerView.ViewHolder holder = this.recyclerView.findViewHolderForAdapterPosition(position);
            if (!(holder instanceof ExerciseViewHolder)) {
                Log.e("DEBUG", "Invalid ViewHolder for position: " + position);
                continue;
            }

            ExerciseViewHolder exerciseHolder = (ExerciseViewHolder) holder;

            List<SetLog> sets = new ArrayList<>();

            // Retrieve reps and weight input from UI fields
            for (int i = 0; i < DEFAULT_SETS; i++) {
                try {
                    int reps = Integer.parseInt(exerciseHolder.setRepsInputs.get(i).getText().toString().trim());
                    float weight = Float.parseFloat(exerciseHolder.setWeightInputs.get(i).getText().toString().trim());

                    sets.add(new SetLog(i + 1, reps, weight));
                    Log.d("DEBUG", "Set " + (i + 1) + ": Reps=" + reps + ", Weight=" + weight);
                } catch (NumberFormatException e) {
                    Log.e("DEBUG", "Error parsing reps/weight for exercise: " + exercise.getName(), e);
                }
            }

            if (!sets.isEmpty()) {
                logs.add(new WorkoutExerciseLog(
                        exercise.getId(),
                        sets,
                        sets.get(sets.size() - 1).getWeight(),
                        sets.get(sets.size() - 1).getReps()
                ));
            } else {
                Log.e("DEBUG", "No valid sets found for exercise: " + exercise.getName());
            }
        }

        if (logs.isEmpty()) {
            Log.e("DEBUG", "No valid workout logs generated!");
        }

        return logs;
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    // ViewHolder class
    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseTitle;
        LinearLayout setsRepsLayout;
        List<EditText> setRepsInputs = new ArrayList<>();
        List<EditText> setWeightInputs = new ArrayList<>();

        ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseTitle = itemView.findViewById(R.id.exercise_title_tv);
            setsRepsLayout = itemView.findViewById(R.id.sets_reps_layout);
        }
    }

    // Show a Dialog with Exercise Details
    private void showExerciseDetailsDialog(Exercise exercise) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(exercise.getName());

        String message = "Target Muscles: " + String.join(", ", exercise.getTargetMuscles()) +
                "\nType: " + exercise.getExerciseType() +
                "\nEquipment: " + exercise.getEquipment() +
                "\nMechanics: " + exercise.getMechanics() +
                "\nInstructions: " + exercise.getInstructions();

        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}