package com.example.vitalmix.api;

import com.example.vitalmix.models.ChangePassword;
import com.example.vitalmix.models.User;
import com.example.vitalmix.models.WorkoutSession;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;


public interface ApiService {

    // Endpoint for user registration
    @POST("/api/auth/register")
    Call<ApiResponse> registerUser(@Body User user);

    // Endpoint for user login
    @POST("/api/auth/login")
    Call<ApiResponse> loginUser(@Body User user);

    @POST("api/auth/questionnaire")
    Call<ApiResponse> saveQuestionnaire(@Body User user);

    // Get workout plan for the logged-in user
    @GET("/api/workout/plan")
    Call<ApiResponse> getWorkoutPlan(@Query("userId") String userId);

    // Get exercises by their IDs
    @GET("/api/exercises/byIds")
    Call<ApiResponse> getExercisesByIds(@Query("exerciseIds") List<String> exerciseIds);

    // Update workout progress (next day index)
    @PUT("/api/workout/progress")
    Call<ApiResponse> updateWorkoutProgress(@Query("userId") String userId, @Query("dayIndex") int dayIndex);

    // Endpoint to save workout session
    @POST("/api/workout/session")
    Call<ApiResponse> saveWorkoutSession(@Body WorkoutSession workoutSession);

    // Get users profile data
    @GET("/api/profile")
    Call<ApiResponse> getUserProfile(@Query("userId") String userId);

    @PUT("/api/profile/changePassword")
    Call<ApiResponse> changePassword(@Body ChangePassword request);

    // Get all default workout plans
    @GET("/api/workout/defaultPlans")
    Call<ApiResponse> getDefaultWorkoutPlans();

    // Get recommended workout plan for a user
    @GET("/api/workout/recommendation")
    Call<ApiResponse> getRecommendedPlan(@Query("userId") String userId);

    @Multipart
    @POST("/predict-form/")
    Call<ApiResponseFastAPI> uploadFormCheck(@Part MultipartBody.Part file, @Part("exercise") RequestBody exercise);
}