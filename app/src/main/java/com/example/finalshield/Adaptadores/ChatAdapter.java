package com.example.finalshield.Adaptadores;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalshield.DTO.Chatbot.ChatMessageDTO;
import com.example.finalshield.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_BOT = 2;

    private List<ChatMessageDTO> messages;

    public ChatAdapter(List<ChatMessageDTO> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {

        ChatMessageDTO message = messages.get(position);

        return message.isUser() ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_USER) {

            View view = inflater.inflate(
                    R.layout.bubble_user,
                    parent,
                    false
            );

            return new UserViewHolder(view);

        } else {

            View view = inflater.inflate(
                    R.layout.bubble_bot,
                    parent,
                    false
            );

            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        ChatMessageDTO message = messages.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);

        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ---------------- USER ----------------
    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView txtMessage;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            txtMessage = itemView.findViewById(R.id.txtMessage);
        }

        void bind(ChatMessageDTO message) {
            txtMessage.setText(message.getMensaje());
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {

        TextView txtMessage;

        public BotViewHolder(@NonNull View itemView) {
            super(itemView);

            txtMessage = itemView.findViewById(R.id.txtMessage);
        }

        void bind(ChatMessageDTO message) {
            txtMessage.setText(message.getMensaje());
        }
    }
}