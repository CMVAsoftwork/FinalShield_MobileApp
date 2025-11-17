package com.example.finalshield.DTO.Usuario;

import com.google.gson.annotations.SerializedName;

public class UsuarioDTO {
    @SerializedName("idUsuario")
    private Integer idUsuario;
    @SerializedName("correo")
    private String correo;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
