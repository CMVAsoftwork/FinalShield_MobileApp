package com.example.finalshield.API;

import com.example.finalshield.DTO.Correo.CorreoDTO;
import com.example.finalshield.DTO.Correo.EnvioCorreoDTO;
import com.example.finalshield.DTO.Correo.RecepcionCorreoDTO;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CorreoAPI {
    @Multipart
    @POST("api/correos/enviar")
    Call<ResponseBody> enviarCorreo(
            @Part("correo") RequestBody correoRequestJson,
            @Part List<MultipartBody.Part> adjuntos
    );

    @GET("api/correos/enviados/{correoUsuario}")
    Call<List<EnvioCorreoDTO>> listarCorreosEnviados(@Path("correoUsuario") String correoUsuario);

    @GET("api/correos/recibidos/{correoUsuario}")
    Call<List<RecepcionCorreoDTO>> listarCorreosRecibidos(@Path("correoUsuario") String correoUsuario);

    @POST("api/correos/adjuntos/descifrar/{idAdjunto}")
    Call<ResponseBody> descifrarAdjunto(
            @Path("idAdjunto") Integer idAdjunto,
            @Body Map<String, String> claveBase64
    );

    @GET("api/enlaces/{token}")
    Call<CorreoDTO> accederClave(
            @Path("token") String token,
            @Query("correoUsuario") String correoUsuario
    );
}
