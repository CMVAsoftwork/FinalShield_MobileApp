package com.example.finalshield.API;

import com.example.finalshield.DTO.EstadisticasResumenDTO;

import retrofit2.Call;
import retrofit2.http.GET;

public interface EstadisticasAPI {
    @GET("api/estadisticas/resumen")
    Call<EstadisticasResumenDTO> obtenerResumen();
}
