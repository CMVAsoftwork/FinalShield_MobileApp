package com.example.finalshield.Adaptadores;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.finalshield.R;
import com.github.chrisbanes.photoview.PhotoView;

public class VistaImagenActivity extends AppCompatActivity {
    PhotoView imagenCompleta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_imagen);
        imagenCompleta = findViewById(R.id.imagenCompleta);
        String uri = getIntent().getStringExtra("uri");
        // Glide sigue funcionando para cargar la imagen en el PhotoView
        Glide.with(this)
                .load(Uri.parse(uri))
                .into(imagenCompleta);
    }
}
