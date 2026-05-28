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
    private static final String BASE_URL = "https://aydan-nonrepresentational-womanishly.ngrok-free.dev/";
    private final EnlaceAPI enlaceAPI;
    private final AuthService authService;
    private final Gson gson;

    public EnlaceService(Context context) {
        this.authService = new AuthService(context);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    String token = authService.obtenerToken();

                    if (token != null) {
                        okhttp3.Request newRequest = chain.request().newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(chain.request());
                })
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(this.gson))
                .build();

        enlaceAPI = retrofit.create(EnlaceAPI.class);
    }

    public EnlaceAPI getAPI() {
        return enlaceAPI;
    }

    public Gson getGson() {
        return gson;
    }
}
