package com.example.finalshield.API;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ArchivoAPI {
    @Multipart
    @POST("api/cifrado/cifrar")
    Call<ResponseBody> cifrarArchivo(
            @Part MultipartBody.Part archivo,
            @Part("claveBase64") RequestBody claveBase64
    );

    @POST("api/cifrado/descifrar")
    Call<ResponseBody> descifrarArchivo(
            @Body Map<String, String> request
    );
}
