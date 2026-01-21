package com.example.finalshield.Model;

public class Faq {
    private String pregunta;
    private String respuesta;
    private int icono; // Nuevo campo

    public Faq(String pregunta, String respuesta, int icono) {
        this.pregunta = pregunta;
        this.respuesta = respuesta;
        this.icono = icono;
    }

    public String getPregunta() { return pregunta; }
    public String getRespuesta() { return respuesta; }
    public int getIcono() { return icono; } // Getter
}
