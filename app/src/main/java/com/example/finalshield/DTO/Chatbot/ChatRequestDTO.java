package com.example.finalshield.DTO.Chatbot;

public class ChatRequestDTO {
    private String message;

    public ChatRequestDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
