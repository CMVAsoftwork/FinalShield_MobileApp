package com.example.finalshield.API;

import com.example.finalshield.DTO.Correo.CorreoDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface EnlaceAPI {
    @GET("api/enlaces/{token}")
    Call<CorreoDTO> accederClave(
            @Path("token") String token,
            @Query("correoUsuario") String correoUsuario
    );
}
