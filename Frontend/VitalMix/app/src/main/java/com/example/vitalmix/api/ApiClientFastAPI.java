package com.example.vitalmix.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientFastAPI {
    private static final String BASE_URL = "http://10.0.2.2:8000/"; // FastAPI Server
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Enable logging for debugging API requests
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Increase timeout limits to prevent connection errors
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS)  // Increase connection timeout
                    .readTimeout(90, TimeUnit.SECONDS)     // Increase read timeout
                    .writeTimeout(90, TimeUnit.SECONDS)    // Increase write timeout
                    .addInterceptor(logging)              // Attach logging interceptor
                    .build();

            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // Attach custom client with timeouts and logging
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
