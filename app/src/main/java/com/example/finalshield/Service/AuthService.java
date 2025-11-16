package com.example.finalshield.Service;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.finalshield.API.AuthAPI;
import com.example.finalshield.DTO.Usuario.LoginBioRequest;
import com.example.finalshield.DTO.Usuario.LoginRequest;
import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.DTO.Usuario.RegistroRequest;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthService {
    private static final String BASE_URL = "https://aydan-nonrepresentational-womanishly.ngrok-free.dev/";
    private static final String PREF_NAME = "finalshield_prefs";
    private static final String TOKEN_KEY = "jwt_token";
    private static final String CORREO_KEY = "correo_usuario";
    private final AuthAPI api;
    private final SharedPreferences prefs;

    public AuthService(Context context) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(AuthAPI.class);
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void login(String correo, String contrasena, Callback<LoginResponse> callback) {
        LoginRequest req = new LoginRequest(correo, contrasena);

        api.login(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarToken(response.body().getToken());
                    guardarCorreo(correo);
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void registro(String nombre, String correo, String contrasena, Callback<LoginResponse> callback) {

        RegistroRequest req = new RegistroRequest(nombre, correo, contrasena);

        api.registro(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarToken(response.body().getToken());
                    guardarCorreo(correo);
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void habilitarBiometrico(String correo, boolean activado, Callback<Void> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("correo", correo);
        body.put("huella", activado);

        api.habilitarBiometrico(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    guardarCorreo(correo);
                    callback.onResponse(null, Response.success(null));
                } else {
                callback.onFailure(null, new RuntimeException("Error HTTP al habilitar biometría: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onFailure(null, t);
            }
        });
    }

    public void loginBiometrico(String correo, Callback<LoginResponse> callback) {
        LoginBioRequest req = new LoginBioRequest(correo);
        api.loginBiometrico(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    guardarToken(response.body().getToken());
                    guardarCorreo(correo);
                }
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    public void isBiometricoActivo(String correo, Callback<Boolean> callback) {
        api.isBiometricoActivo(correo).enqueue(callback);
    }

    public void guardarToken(String token) {
        prefs.edit().putString(TOKEN_KEY, token).apply();
    }

    public String obtenerToken() {
        return prefs.getString(TOKEN_KEY, null);
    }

    public void guardarCorreo(String correo) {
        prefs.edit().putString(CORREO_KEY, correo).apply();
    }

    public String obtenerCorreo() {
        return prefs.getString(CORREO_KEY, null);
    }

    public void cerrarSesion() {
        prefs.edit().remove(TOKEN_KEY).apply();
    }
}
