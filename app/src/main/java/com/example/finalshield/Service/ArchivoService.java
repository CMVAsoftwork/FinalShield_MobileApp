package com.example.finalshield.Service;

import android.content.Context;
import android.util.Log;

import com.example.finalshield.API.ArchivoAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ArchivoService {

    private final ArchivoAPI archivoAPI;
    private final Gson gson;

    public ArchivoService(Context context) {

        this.gson = new GsonBuilder()
                .setLenient()
                .create();

        archivoAPI = RetrofitClient
                .getInstance(context)
                .create(ArchivoAPI.class);
    }

    public ArchivoAPI getAPI() {
        return archivoAPI;
    }

    public Gson getGson() {
        return gson;
    }
}
