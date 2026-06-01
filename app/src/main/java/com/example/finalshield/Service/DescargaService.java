package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.DescargaAPI;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DescargaService {

    private final DescargaAPI descargaAPI;

    public DescargaService(Context context) {

        descargaAPI = RetrofitClient
                .getInstance(context)
                .create(DescargaAPI.class);
    }

    public DescargaAPI getAPI() {
        return descargaAPI;
    }
}
