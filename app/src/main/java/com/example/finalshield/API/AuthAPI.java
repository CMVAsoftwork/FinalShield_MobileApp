package com.example.finalshield.API;

import com.example.finalshield.DTO.Usuario.LoginBioRequest;
import com.example.finalshield.DTO.Usuario.LoginRequest;
import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.DTO.Usuario.RegistroRequest;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AuthAPI {
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/registro")
    Call<LoginResponse> registro(@Body RegistroRequest request);

    @POST("api/auth/habilitar-biometrico")
    Call<ResponseBody> habilitarBiometrico(@Body Map<String, Object> body);

    @POST("api/auth/login-bio")
    Call<LoginResponse> loginBiometrico(@Body LoginBioRequest request);

    @GET("api/auth/biometrico-activo/{correo}")
    Call<Boolean> isBiometricoActivo(@Path("correo") String correo);

}
