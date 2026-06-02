package com.example.finalshield.API;

import com.example.finalshield.DTO.Chatbot.ChatHistoryDTO;
import com.example.finalshield.DTO.Chatbot.ChatRequestDTO;
import com.example.finalshield.DTO.Chatbot.ChatResponseDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ChatAPI
{
    @POST("/api/chat")
    Call<ChatResponseDTO> sendMessage(@Body ChatRequestDTO request);

    @GET("/api/chat/history")
    Call<List<ChatHistoryDTO>> getHistory();
}
