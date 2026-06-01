package com.example.finalshield.Service;

import android.content.Context;

import com.example.finalshield.API.ChatAPI;
import com.example.finalshield.DTO.Chatbot.ChatRequestDTO;
import com.example.finalshield.DTO.Chatbot.ChatResponseDTO;

import retrofit2.Call;
import retrofit2.Callback;

public class ChatService {

    private ChatAPI chatAPI;

    public ChatService(Context context)
    {
        chatAPI=RetrofitClient.getInstance(context).create(ChatAPI.class);
    }

    public void enviarMensaje(String mensaje, Callback<ChatResponseDTO> callback)
    {
        ChatRequestDTO requestDTO=new ChatRequestDTO(mensaje);

        chatAPI.sendMessage(requestDTO).enqueue(callback);
    }
}
