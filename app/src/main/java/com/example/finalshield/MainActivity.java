package com.example.finalshield;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalshield.Fragments.Chatbot.ChatFragmento;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {

    // Variable global para almacenar el desvío de pantalla pendiente
    private String destinoPendiente = null;
    private boolean chatAbierto=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (getIntent() != null && getIntent().hasExtra("pantalla_destino")) {
            destinoPendiente = getIntent().getStringExtra("pantalla_destino");
            Log.d("FINALSHIELD_LINK", "Destino capturado en onCreate: " + destinoPendiente);
        }

        procesarVinculo(getIntent());

        ImageView btnChat=findViewById(R.id.fabChat);
        FrameLayout contenedorChat=findViewById(R.id.chatContainer);

        btnChat.setOnClickListener(v -> {
            if (!chatAbierto) {

                contenedorChat.setVisibility(View.VISIBLE);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.chatContainer, new ChatFragmento())
                        .commit();

                btnChat.setVisibility(View.GONE);

                chatAbierto = true;

            } else {

                contenedorChat.setVisibility(View.GONE);
                btnChat.setVisibility(View.VISIBLE);
                chatAbierto = false;
            }

        });

        contenedorChat.setOnClickListener(v -> {

            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(
                            getSupportFragmentManager()
                                    .findFragmentById(R.id.chatContainer)
                    )
                    .commit();

            contenedorChat.setVisibility(View.GONE);

            btnChat.setVisibility(View.VISIBLE);

            Log.d("CHAT_TEST", "CLICK DETECTADO");
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // 2. Capturamos el desvío si la app se despertó estando en segundo plano
        if (intent != null && intent.hasExtra("pantalla_destino")) {
            destinoPendiente = intent.getStringExtra("pantalla_destino");
            Log.d("FINALSHIELD_LINK", "Destino capturado en onNewIntent: " + destinoPendiente);
        }

        procesarVinculo(intent);
    }

    private void procesarVinculo(Intent intent) {
        Log.d("FINALSHIELD_LINK", "Intent recibido: " + (intent != null));
        if (intent != null) {
            Log.d("FINALSHIELD_LINK", "Data del intent: " + intent.getData());
            Uri data = intent.getData();
            if (data != null) {
                String token = data.getQueryParameter("security_token");
                Log.d("FINALSHIELD_LINK", "Token extraído: " + token);

                if (token != null && !token.isEmpty()) {
                    getSharedPreferences("deep_link", Context.MODE_PRIVATE)
                            .edit()
                            .putString("pending_token", token)
                            .commit();
                    Log.d("FINALSHIELD_LINK", "TOKEN GUARDADO EXITOSAMENTE");
                }
            }
        }
    }

    // --- MÉTODOS PÚBLICOS DE ACCESO PARA TU FRAGMENT DE LOGIN ---
    public String getDestinoPendiente() {
        return destinoPendiente;
    }

    public void limpiarDestinoPendiente() {
        this.destinoPendiente = null;
    }
}