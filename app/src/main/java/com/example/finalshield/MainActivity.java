package com.example.finalshield;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Variable global para almacenar el desvío de pantalla pendiente
    private String destinoPendiente = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1. Verificamos si se inyectó una orden de desvío desde las notificaciones
        if (getIntent() != null && getIntent().hasExtra("pantalla_destino")) {
            destinoPendiente = getIntent().getStringExtra("pantalla_destino");
            Log.d("FINALSHIELD_LINK", "Destino capturado en onCreate: " + destinoPendiente);
        }

        procesarVinculo(getIntent());
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