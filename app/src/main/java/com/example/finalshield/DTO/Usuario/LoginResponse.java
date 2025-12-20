package com.example.finalshield.DTO.Usuario;

public class LoginResponse {
    private String token;
    private String tipoToken;
    private String correo;
    private String nombre;
    private Integer idUsuario;
    private boolean biometricoActivo;

    public String getNombre() {
        return nombre;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getToken() {
        return token;
    }

    public String getTipoToken() {
        return tipoToken;
    }

    public String getCorreo() {
        return correo;
    }
    public boolean isBiometricoActivo() {
        return biometricoActivo;
    }
}
