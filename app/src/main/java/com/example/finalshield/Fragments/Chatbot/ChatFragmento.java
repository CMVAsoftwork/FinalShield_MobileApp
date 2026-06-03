package com.example.finalshield.Fragments.Chatbot;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ChatAdapter;
import com.example.finalshield.DTO.Chatbot.ChatHistoryDTO;
import com.example.finalshield.DTO.Chatbot.ChatMessageDTO;
import com.example.finalshield.DTO.Chatbot.ChatResponseDTO;
import com.example.finalshield.Fragments.Perfil;
import com.example.finalshield.R;
import com.example.finalshield.Service.ChatService;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ChatFragmento extends BottomSheetDialogFragment {


    private RecyclerView rvChat;
    private EditText txtMessage;
    private ImageView btnEnviar, btnUsuario, btnMenu;
    private ChatAdapter chatAdapter;
    private List<ChatMessageDTO> messages;
    private Handler typingHandler=new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;

    private ChatService chatService;
    public ChatFragmento(){
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_fragmento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChat=view.findViewById(R.id.rvChat);
        txtMessage=view.findViewById(R.id.etMessage);
        btnEnviar=view.findViewById(R.id.btnSend);
        btnMenu=view.findViewById(R.id.menu);
        btnUsuario=view.findViewById(R.id.userCircle);

        chatService=new ChatService(requireContext());
        messages=new ArrayList<>();

        chatAdapter=new ChatAdapter(messages);

        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));

        rvChat.setAdapter(chatAdapter);

        btnEnviar.setOnClickListener(v->{
            enviarMensaje();
        });

        btnMenu.setOnClickListener(v->{
            requireActivity()
                    .findViewById(R.id.chatContainer)
                    .setVisibility(View.GONE);
            requireActivity()
                    .findViewById(R.id.fabChat)
                    .setVisibility(View.VISIBLE);

        });

        view.setOnClickListener(v->{
            view.setClickable(true);
        });

        btnUsuario.setOnClickListener(v->{
            requireActivity()
                    .getSupportFragmentManager().beginTransaction().replace(R.id.contenedorp, new Perfil())
                    .addToBackStack(null).commit();

        });

        View fondo=view.findViewById(R.id.fondoChat);
        fondo.setOnClickListener(v->{

            requireActivity().findViewById(R.id.chatContainer)
                    .setVisibility(View.GONE);

            requireActivity().findViewById(R.id.fabChat)
                    .setVisibility(View.VISIBLE);
        });

        ConstraintLayout ventana=view.findViewById(R.id.ventanaChat);
        ventana.setOnClickListener(v->{

        });
        cargarHistorial();
    }

    private void enviarMensaje()
    {
        String mensaje= txtMessage.getText().toString().trim();

        messages.add(new ChatMessageDTO(mensaje, true));

        chatAdapter.notifyItemInserted(messages.size()-1);

        rvChat.smoothScrollToPosition(messages.size()-1);
        txtMessage.setText("");

        ChatMessageDTO typingMsg=new ChatMessageDTO("FinalBot está escribiendo.", false);

        messages.add(typingMsg);
        chatAdapter.notifyItemInserted(messages.size()-1);
        rvChat.scrollToPosition(messages.size()-1);

        typingRunnable = new Runnable() {
            String[] estados = {
                    "●",
                    "● ●",
                    "● ● ●"
            };
            int indice = 0;

            @Override
            public void run() {

                typingMsg.setMensaje(
                        estados[indice]
                );

                int pos = messages.indexOf(typingMsg);

                if(pos != -1)
                {
                    chatAdapter.notifyItemChanged(pos);
                }

                indice++;

                if(indice >= estados.length)
                {
                    indice = 0;
                }

                typingHandler.postDelayed(this, 450);
            }
        };
        typingHandler.post(typingRunnable);
        if (mensaje.isEmpty())
            return;

        chatService.enviarMensaje(mensaje, new Callback<ChatResponseDTO>() {
            @Override
            public void onResponse(Call<ChatResponseDTO> call, Response<ChatResponseDTO> response) {
                if (response.isSuccessful() && response.body()!=null)
                {
                    String respuestaBot=response.body().getResponse();

                    typingHandler.removeCallbacks(typingRunnable);
                    messages.remove(typingMsg);
                    chatAdapter.notifyDataSetChanged();

                    ChatMessageDTO botMsg=new ChatMessageDTO(respuestaBot, false);
                    messages.add(botMsg);

                    chatAdapter.notifyItemInserted(messages.size()-1);
                    rvChat.scrollToPosition(messages.size()-1);
                }
                else {
                    ChatMessageDTO errorMsg=new ChatMessageDTO("Error del servidor", false);
                    messages.add(errorMsg);

                    chatAdapter.notifyItemInserted(messages.size()-1);
                }
            }

            @Override
            public void onFailure(Call<ChatResponseDTO> call, Throwable t) {

                Log.e(
                        "CHAT_ERROR",
                        "Retrofit",
                        t
                );

                t.printStackTrace();

                ChatMessageDTO errorMsg=new ChatMessageDTO("Sin conexion con el servidor", false);

                typingHandler.removeCallbacks(typingRunnable);
                messages.remove(typingMsg);
                chatAdapter.notifyDataSetChanged();
                rvChat.scrollToPosition(messages.size()-1);
                messages.add(errorMsg);
                chatAdapter.notifyItemInserted(messages.size()-1);
            }
        });
    }

    private void cargarHistorial() {
        chatService.obtenerHistorial(new Callback<List<ChatHistoryDTO>>() {
            @Override
            public void onResponse(Call<List<ChatHistoryDTO>> call, Response<List<ChatHistoryDTO>> response) {
                if(response.isSuccessful() && response.body() !=null)
                {
                    messages.clear();

                    for (ChatHistoryDTO h : response.body()){
                        messages.add(new ChatMessageDTO(h.getMensaje(), true));

                        messages.add(new ChatMessageDTO(h.getRespuesta(), false));
                    }
                    chatAdapter.notifyDataSetChanged();

                    rvChat.scrollToPosition(messages.size()-1);
                }
            }

            @Override
            public void onFailure(Call<List<ChatHistoryDTO>> call, Throwable t) {
                Log.d("HISTORY", t.getMessage());
            }
        });
    }
}