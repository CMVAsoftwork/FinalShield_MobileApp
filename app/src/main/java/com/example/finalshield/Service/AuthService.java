package com.example.finalshield.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.finalshield.API.AuthAPI;
import com.example.finalshield.DTO.Usuario.CambiarContraseñaRequest;
import com.example.finalshield.DTO.Usuario.LoginBioRequest;
import com.example.finalshield.DTO.Usuario.LoginRequest;
import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.DTO.Usuario.RegistroRequest;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
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
    private static final String NOMBRE_KEY = "nombre_usuario";
    private final AuthAPI api;
    private final SharedPreferences prefs;

    public AuthService(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    String token = obtenerToken();
                    Request.Builder builder = chain.request().newBuilder();
                    if (token != null) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(AuthAPI.class);
    }

    public void login(String correo, String contrasena, Callback<LoginResponse> callback) {
        LoginRequest req = new LoginRequest(correo, contrasena);

        api.login(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("DEBUG_AUTH", "Token recibido: " + response.body().getToken());
                    Log.d("DEBUG_AUTH", "Nombre recibido del server: " + response.body().getNombre());
                    Log.d("DEBUG_AUTH", "Correo recibido del server: " + response.body().getCorreo());
                    guardarToken(response.body().getToken());
                    guardarCorreo(response.body().getCorreo());
                    guardarNombre(response.body().getNombre());
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
                    guardarCorreo(response.body().getCorreo());
                    guardarNombre(response.body().getNombre());
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
                    guardarNombre(response.body().getNombre());
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
        prefs.edit().putString(TOKEN_KEY, token).commit();
    }

    public String obtenerToken() {
        return prefs.getString(TOKEN_KEY, null);
    }

    public void guardarCorreo(String correo) {
        prefs.edit().putString(CORREO_KEY, correo).commit();
    }

    public String obtenerCorreo() {
        return prefs.getString(CORREO_KEY, null);
    }

    public void guardarNombre(String nombre) {
        prefs.edit().putString(NOMBRE_KEY, nombre).apply();
    }

    public String obtenerNombre() {
        return prefs.getString(NOMBRE_KEY, "Usuario");
    }

    public boolean isLoggedIn() {
        return obtenerToken() != null;
    }

    public void cerrarSesion() {
        prefs.edit().remove(TOKEN_KEY).commit();
    }

    public void cambiarContraseña(String contrasenaActual, String nuevaContrasena, Callback<ResponseBody> callback) {
        CambiarContraseñaRequest req = new CambiarContraseñaRequest(contrasenaActual, nuevaContrasena);

        api.cambiarContraseña(req).enqueue(callback);
    }
}
