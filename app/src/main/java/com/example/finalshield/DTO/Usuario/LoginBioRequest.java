package com.example.finalshield.DTO.Usuario;

public class LoginBioRequest {
    private String correo;

    public LoginBioRequest(String correo) {
        this.correo = correo;
    }

    public String getCorreo() {
        return correo;
    }
}
