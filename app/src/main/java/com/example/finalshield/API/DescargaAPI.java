package com.example.finalshield.API;

import com.example.finalshield.DTO.DescifradoRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DescargaAPI {
    @POST("api/correos/adjuntos/descifrar/{idAdjunto}")
    Call<ResponseBody> descifrarAdjunto(
            @Path("idAdjunto") Integer idAdjunto,
            @Body DescifradoRequest request
    );
}
