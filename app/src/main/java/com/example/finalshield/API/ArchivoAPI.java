package com.example.finalshield.API;

import com.example.finalshield.Model.Archivo;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ArchivoAPI {

    @Multipart
    @POST("api/cifrado/cifrar")
    Call<List<Archivo>> cifrarArchivos(@Part List<MultipartBody.Part> files);

    @GET("api/cifrado/descargarCifrado/{idArchivo}")
    Call<ResponseBody> descargarCifrado(@Path("idArchivo") int idArchivo);

    @GET("api/cifrado/list")
    Call<List<Archivo>> getAllArchivos();

    @DELETE("api/cifrado/borrar/{idArchivo}")
    Call<Void> borrarArchivo(@Path("idArchivo") int idArchivo);

    @GET("api/cifrado/descifrarArchivo/{idArchivo}")
    Call<ResponseBody> descifrarArchivo(@Path("idArchivo") int idArchivo);  // Nuevo
}