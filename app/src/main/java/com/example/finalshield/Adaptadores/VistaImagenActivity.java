package com.example.finalshield.Adaptadores;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.finalshield.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

public class VistaImagenActivity extends AppCompatActivity {

    public static final String EXTRA_URI_LIST = "uri_list";
    public static final String EXTRA_POSITION = "position";
    private static final String SOURCE_KEY = "source_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_imagen);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        List<Uri> finalUriList = null;
        int startPosition = 0;

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "Error: No se recibieron datos de inicio.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        startPosition = extras.getInt(EXTRA_POSITION, 0);

        // 1. Intentar obtener la lista del Intent (MÉTODO COMPATIBLE)
        ArrayList<String> uriStringListFromIntent = extras.getStringArrayList(EXTRA_URI_LIST);

        if (uriStringListFromIntent != null && !uriStringListFromIntent.isEmpty()) {
            // Conversión de String a Uri
            finalUriList = new ArrayList<>();
            for (String uriString : uriStringListFromIntent) {
                finalUriList.add(Uri.parse(uriString));
            }

        } else if ("SHARED_VIEWMODEL".equals(extras.getString(SOURCE_KEY))) {

            // 2. FALLBACK: Cargar del ViewModel (SOLUCIÓN A TransactionTooLarge)
            SharedImageViewModel sharedViewModel = new ViewModelProvider(this).get(SharedImageViewModel.class);
            finalUriList = sharedViewModel.getImageUriList();

            // Limpiamos el ViewModel inmediatamente para liberar memoria.
            sharedViewModel.clearList();
        }

        if (finalUriList != null && !finalUriList.isEmpty()) {

            // Asegurar que la posición sea válida
            if (startPosition < 0 || startPosition >= finalUriList.size()) {
                startPosition = 0;
            }

            // 3. Configurar el adaptador (La Activity debe ser pasada como FragmentActivity)
            // Asegúrate de que ImagePagerAdapter acepte List<Uri>
            ImagePagerAdapter adapter = new ImagePagerAdapter(this, finalUriList);
            viewPager.setAdapter(adapter);
            viewPager.setCurrentItem(startPosition, false);

        } else {
            // Este es el Toast de error final si todo falla
            Toast.makeText(this, "Error: No se pudieron cargar las imágenes.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
