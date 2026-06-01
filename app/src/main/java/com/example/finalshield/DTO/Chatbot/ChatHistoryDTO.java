package com.example.finalshield.DTO.Chatbot;

public class ChatHistoryDTO
{
    private String mensaje;
    private String respuesta;
    private Boolean fallback;
    private String fecha;

    public String getMensaje() {
        return mensaje;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public Boolean getFallback() {
        return fallback;
    }

    public String getFecha() {
        return fecha;
    }
}
