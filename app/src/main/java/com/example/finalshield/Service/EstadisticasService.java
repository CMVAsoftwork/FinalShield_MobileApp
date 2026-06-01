package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.EstadisticasAPI;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EstadisticasService {

    private final EstadisticasAPI estadisticasAPI;

    public EstadisticasService(Context context) {

        estadisticasAPI = RetrofitClient
                .getInstance(context)
                .create(EstadisticasAPI.class);
    }

    public EstadisticasAPI getAPI() {
        return estadisticasAPI;
    }
}