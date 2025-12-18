package com.example.finalshield.DTO.Usuario;

public class CambiarContraseñaRequest {
    String contraseña;
    String correo;
    String contraseñaActual;

    public CambiarContraseñaRequest(String contraseña, String correo, String contraseñaActual) {
        this.contraseña = contraseña;
        this.correo = correo;
        this.contraseñaActual = contraseñaActual;
    }

    public String getContraseña() {
        return contraseña;
    }

    public void setContraseña(String contraseña) {
        this.contraseña = contraseña;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContraseñaActual() {
        return contraseñaActual;
    }

    public void setContraseñaActual(String contraseñaActual) {
        this.contraseñaActual = contraseñaActual;
    }
}
