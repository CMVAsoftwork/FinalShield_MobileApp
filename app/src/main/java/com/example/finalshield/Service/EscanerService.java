package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.EscanerAPI;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EscanerService {
    private static final String BASE_URL =
            "https://aydan-nonrepresentational-womanishly.ngrok-free.dev/";

    private final EscanerAPI escanerAPI;

    public EscanerService(Context context) {
        AuthService authService = new AuthService(context);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    String token = authService.obtenerToken();
                    if (token != null) {
                        return chain.proceed(
                                chain.request().newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .build()
                        );
                    }
                    return chain.proceed(chain.request());
                })
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        escanerAPI = retrofit.create(EscanerAPI.class);
    }

    public EscanerAPI getAPI() {
        return escanerAPI;
    }
}
