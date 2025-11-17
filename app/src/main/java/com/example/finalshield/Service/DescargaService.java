package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.DescargaAPI;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DescargaService {
    private static final String BASE_URL = "https://aydan-nonrepresentational-womanishly.ngrok-free.dev/";

    private final DescargaAPI descargaAPI;
    private final AuthService authService;

    public DescargaService(Context context) {
        this.authService = new AuthService(context);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

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
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        descargaAPI = retrofit.create(DescargaAPI.class);
    }

    public DescargaAPI getAPI() {
        return descargaAPI;
    }
}
