package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.EscanerAPI;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EscanerService {

    private final EscanerAPI escanerAPI;

    public EscanerService(Context context) {

        escanerAPI = RetrofitClient
                .getInstance(context)
                .create(EscanerAPI.class);
    }

    public EscanerAPI getAPI() {
        return escanerAPI;
    }
}