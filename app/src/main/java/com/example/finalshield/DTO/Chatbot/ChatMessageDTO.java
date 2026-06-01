package com.example.finalshield.DTO.Chatbot;

public class ChatMessageDTO {
    private String mensaje;
    private boolean isUser;

    public ChatMessageDTO(String mensaje, boolean isUser) {
        this.mensaje = mensaje;
        this.isUser = isUser;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isUser() {
        return isUser;
    }
}
