package com.example.vitalmix.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vitalmix.R;
import com.example.vitalmix.api.ApiClient;
import com.example.vitalmix.api.ApiResponse;
import com.example.vitalmix.auth.SessionManager;
import com.example.vitalmix.models.DefaultWorkoutPlan;
import com.example.vitalmix.ui.StartWorkoutActivity;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DefaultWorkoutPlanAdapter extends RecyclerView.Adapter<DefaultWorkoutPlanAdapter.PlanViewHolder> {

    private final Context context;
    private final List<DefaultWorkoutPlan> planList;

    // Recommended plan ID to be highlighted set after returning default
    private String recommendedPlanId = null;

    // Constructor (only plans passed here now)
    public DefaultWorkoutPlanAdapter(Context context, List<DefaultWorkoutPlan> planList) {
        this.context = context;
        this.planList = planList;
    }

    // Setter for recommended plan ID (can be called after fetching recommendation)
    public void setRecommendedPlanId(String recommendedPlanId) {
        this.recommendedPlanId = recommendedPlanId;
        notifyDataSetChanged(); // Refresh views to apply highlight
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the row layout
        View view = LayoutInflater.from(context).inflate(R.layout.default_workout_plan_row, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        // Get current plan
        DefaultWorkoutPlan plan = planList.get(position);

        // Set plan name and description
        holder.planTitleTv.setText(plan.getPlanName());
        holder.planDescTv.setText(plan.getDescription());

        // Highlight if this is the recommended plan
        if (recommendedPlanId != null && plan.getId().equals(recommendedPlanId)) {
            holder.recommendedLabelTv.setVisibility(View.VISIBLE);
        } else {
            holder.recommendedLabelTv.setVisibility(View.GONE);
        }

        // Handle choose button click
        holder.choosePlanBtn.setOnClickListener(v -> {
            Toast.makeText(context, "Chosen: " + plan.getPlanName(), Toast.LENGTH_SHORT).show();

            // Prep request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("userId", SessionManager.getLoggedInUserID(context));
            requestBody.addProperty("chosenPlanId", plan.getId());
            Log.d("workout_debug", "Request JSON: " + requestBody.toString());

            // Call the backend to assign the chosen plan
            ApiClient.getApiService().chooseWorkoutPlan(requestBody).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    Log.d("workout_debug", "Response raw body: " + ApiClient.getGson().toJson(response.body()));
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        // Show success message
                        Toast.makeText(context, "Plan assigned successfully!", Toast.LENGTH_SHORT).show();

                        // Navigate to StartWorkoutActivity
                        Intent intent = new Intent(context, StartWorkoutActivity.class);
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, "Failed to assign plan", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Also HANDLE general row click, maybe display exercises that are in it?

    @Override
    public int getItemCount() {
        return planList.size();
    }

    // ViewHolder holds row views for recycling
    static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView planTitleTv, planDescTv, recommendedLabelTv;
        Button choosePlanBtn;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            planTitleTv = itemView.findViewById(R.id.plan_name_tv);
            planDescTv = itemView.findViewById(R.id.plan_description_tv);
            recommendedLabelTv = itemView.findViewById(R.id.recommended_label_tv);
            choosePlanBtn = itemView.findViewById(R.id.choose_plan_btn);
        }
    }
}

