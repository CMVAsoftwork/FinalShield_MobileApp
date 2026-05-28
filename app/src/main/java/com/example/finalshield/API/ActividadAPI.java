package com.example.finalshield.API;

import com.example.finalshield.DTO.ActividadArchivoDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ActividadAPI {
    @GET("api/estadisticas/actividad")
    Call<List<ActividadArchivoDTO>> obtenerActividad();
}
