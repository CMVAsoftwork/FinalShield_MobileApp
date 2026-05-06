package com.example.finalshield.DTO.Usuario;

public class RegistroRequest {
    private String nombre;
    private String correo;
    private String contrasena;
    private String pinReal;
    private String pinSeguro;
    private boolean huella;

    public RegistroRequest(String nombre, String correo, String contrasena, String pinReal, String pinSeguro) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.pinReal = pinReal;
        this.pinSeguro = pinSeguro;
        this.huella = huella;
    }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public String getPinReal() { return pinReal; }
    public String getPinSeguro() { return pinSeguro; }
    public boolean isHuella() { return huella; }
}