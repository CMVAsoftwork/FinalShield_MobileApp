package com.example.finalshield.DTO.Usuario;

import com.google.gson.annotations.SerializedName;

public class CambiarContraseñaRequest {
    @SerializedName("correo")
    private String correo;

    @SerializedName("contrasenaActual")
    private String contrasenaActual;

    @SerializedName("nuevaContrasena")
    private String nuevaContrasena;

    // Constructor actualizado para incluir el correo
    public CambiarContraseñaRequest(String correo, String contrasenaActual, String nuevaContrasena) {
        this.correo = correo;
        this.contrasenaActual = contrasenaActual;
        this.nuevaContrasena = nuevaContrasena;
    }

    // Getters y Setters
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNuevaContrasena() { return nuevaContrasena; }
    public void setNuevaContrasena(String nuevaContrasena) { this.nuevaContrasena = nuevaContrasena; }

    public String getContrasenaActual() { return contrasenaActual; }
    public void setContrasenaActual(String contrasenaActual) { this.contrasenaActual = contrasenaActual; }
}