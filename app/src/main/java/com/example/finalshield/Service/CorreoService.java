package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.CorreoAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CorreoService {

    private final CorreoAPI correoAPI;
    private final Gson gson;

    public CorreoService(Context context) {

        this.gson = new GsonBuilder()
                .setLenient()
                .create();

        correoAPI = RetrofitClient
                .getInstance(context)
                .create(CorreoAPI.class);
    }

    public CorreoAPI getAPI() {
        return correoAPI;
    }

    public Gson getGson() {
        return gson;
    }
}