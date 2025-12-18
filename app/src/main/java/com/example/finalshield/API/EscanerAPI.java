package com.example.finalshield.API;

import com.example.finalshield.DTO.ArchivoDTO;
import com.example.finalshield.DTO.Escaner.ArchivoEscaneadoDTO;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface EscanerAPI {
    @Multipart
    @POST("api/escaner/cifrar")
    Call<List<ArchivoDTO>> escanearYCifrar(
            @Part("idUsuario") RequestBody idUsuario,
            @Part("contrasena") RequestBody contrasena,
            @Part("archivos") List<MultipartBody.Part> archivos,
            @Part("idCarpeta") RequestBody idCarpeta
    );
}
