package com.example.finalshield.Adaptadores;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalshield.R;

public class VistaImagenActivity extends AppCompatActivity {
    ImageView imagenCompleta;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_imagen);
        imagenCompleta = findViewById(R.id.imagenCompleta);
        String uri = getIntent().getStringExtra("uri");
        imagenCompleta.setImageURI(Uri.parse(uri));
    }
}
