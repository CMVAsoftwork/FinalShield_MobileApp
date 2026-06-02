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

    private static final String TOKEN_KEY = "jwt_token";
    private static final String CORREO_KEY = "correo_usuario";
    private static final String NOMBRE_KEY = "nombre_usuario";

    private static final String PIN_REAL_KEY = "pin_real";
    private static final String PIN_SEGURO_KEY = "pin_seguro";


    private final AuthAPI api;
    private final SharedPreferences prefs;

    public AuthService(Context context) {

            prefs = context.getSharedPreferences("finalshield_prefs", Context.MODE_PRIVATE);

            api = RetrofitClient
                    .getInstance(context)
                    .create(AuthAPI.class);
        }

        // --- LOGIN Y REGISTRO (CON SOPORTE PARA PINES) ---

        public void login(String correo, String contrasena, Callback<LoginResponse> callback) {
            LoginRequest req = new LoginRequest(correo, contrasena);
            api.login(req).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        guardarToken(response.body().getToken());
                        guardarCorreo(response.body().getCorreo());
                        guardarNombre(response.body().getNombre());

                        guardarPinReal(response.body().getPinReal());
                        guardarPinSeguro(response.body().getPinSeguro());

                        Log.d("JWT", TOKEN_KEY);
                    }
                    callback.onResponse(call, response);
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    callback.onFailure(call, t);
                }
            });
        }

        public void registro(String nombre, String correo, String contrasena, String pinR, String pinS, Callback<LoginResponse> callback) {
            // Registro enfocado en los 5 campos principales
            RegistroRequest req = new RegistroRequest(nombre, correo, contrasena, pinR, pinS);

            api.registro(req).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        guardarToken(response.body().getToken());
                        guardarCorreo(response.body().getCorreo());
                        guardarNombre(response.body().getNombre());

                        guardarPinReal(pinR);
                        guardarPinSeguro(pinS);
                    }
                    callback.onResponse(call, response);
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    callback.onFailure(call, t);
                }
            });
        }

        // --- MÉTODOS DE PIN PARA LA CLASE INICIO ---
        public String obtenerPinReal() {
            return prefs.getString(PIN_REAL_KEY, "");
        }

        public String obtenerPinSeguro() {
            return prefs.getString(PIN_SEGURO_KEY, "");
        }

        private void guardarPinReal(String pin) {
            if (pin != null) prefs.edit().putString(PIN_REAL_KEY, pin).apply();
        }

        private void guardarPinSeguro(String pin) {
            if (pin != null) prefs.edit().putString(PIN_SEGURO_KEY, pin).apply();
        }

        // --- MÉTODOS DE PERSISTENCIA (TOKEN, CORREO, NOMBRE) ---
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

        // --- GESTIÓN DE SESIÓN (RESTAURADO) ---
        public boolean isLoggedIn() {
            return obtenerToken() != null;
        }

        public void cerrarSesion() {
            // Usamos remove para el token como lo tenías originalmente
            prefs.edit().remove(TOKEN_KEY).commit();
        }

        // --- CAMBIO DE CONTRASEÑA (RESTAURADO) ---
        public void cambiarContraseña(String actual, String nueva, Callback<ResponseBody> callback) {
            // 1. Recuperamos el correo que guardamos al iniciar sesión
            String correoCargado = obtenerCorreo();

            // 2. Creamos el DTO con los 3 parámetros que espera tu API
            // Asegúrate de que tu clase CambiarContraseñaRequest acepte (correo, actual, nueva)
            CambiarContraseñaRequest req = new CambiarContraseñaRequest(correoCargado, actual, nueva);

            Log.d("DEBUG_AUTH", "Enviando cambio de contraseña para: " + correoCargado);

            // 3. Ejecutamos la petición
            api.cambiarContraseña(req).enqueue(callback);
        }

        // --- BIOMETRÍA ---
        public void habilitarBiometrico(String correo, boolean activado, Callback<Void> callback) {
            Map<String, Object> body = new HashMap<>();
            body.put("correo", correo);
            body.put("huella", activado);
            api.habilitarBiometrico(body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        guardarCorreo(correo); // Aseguramos persistencia del correo al activar
                        callback.onResponse(null, Response.success(null));
                    } else {
                        callback.onFailure(null, new RuntimeException("Error: " + response.code()));
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
                        // Guardar datos básicos
                        guardarToken(response.body().getToken());
                        guardarCorreo(correo);
                        guardarNombre(response.body().getNombre());

                        // GUARDAR TODO CON COMMIT (Para que sea instantáneo)
                        // Nota: usamos nombres de strings directos para evitar errores si no están las constantes
                        prefs.edit()
                                .putString("pin_real", response.body().getPinReal())
                                .putString("pin_seguro", response.body().getPinSeguro())
                                .commit();

                        Log.d("DEBUG_AUTH", "Login Bio exitoso y datos persistidos.");
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
    }
