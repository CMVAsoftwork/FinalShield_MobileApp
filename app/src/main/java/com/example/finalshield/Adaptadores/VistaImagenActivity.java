package com.example.finalshield.Adaptadores;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.finalshield.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;

public class VistaImagenActivity extends AppCompatActivity {

    // Claves para recibir la lista y la posición
    public static final String EXTRA_URI_LIST = "uri_list";
    public static final String EXTRA_POSITION = "position";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_imagen);

        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // 1. Obtener la lista completa de URIs y la posición inicial
        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        // La lista viene como ArrayList<String>
        ArrayList<String> uriList = extras.getStringArrayList(EXTRA_URI_LIST);
        // La posición es el índice de la imagen clickeada
        int startPosition = extras.getInt(EXTRA_POSITION, 0);

        if (uriList != null && !uriList.isEmpty()) {
            // 2. Configurar el adaptador
            ImagePagerAdapter adapter = new ImagePagerAdapter(this, uriList);
            viewPager.setAdapter(adapter);

            // 3. Mostrar la imagen inicial
            viewPager.setCurrentItem(startPosition, false);
        }
    }
}
