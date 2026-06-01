package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.EnlaceAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EnlaceService {

    private final EnlaceAPI enlaceAPI;
    private final Gson gson;

    public EnlaceService(Context context) {

        this.gson = new GsonBuilder()
                .setLenient()
                .create();

        enlaceAPI = RetrofitClient
                .getInstance(context)
                .create(EnlaceAPI.class);
    }

    public EnlaceAPI getAPI() {
        return enlaceAPI;
    }

    public Gson getGson() {
        return gson;
    }
}