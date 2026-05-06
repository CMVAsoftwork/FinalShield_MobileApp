package com.example.finalshield;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Llamamos al único método de procesamiento
        procesarVinculo(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
}