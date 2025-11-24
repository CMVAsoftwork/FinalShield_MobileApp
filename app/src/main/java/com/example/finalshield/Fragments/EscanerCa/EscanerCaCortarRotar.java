package com.example.finalshield.Fragments.EscanerCa;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageView;
import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EscanerCaCortarRotar extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    // Vistas
    ImageButton cam1, eliminar1;
    private Button regresarBtn, guardarBtn;
    private Button cortarBtn, girar90Btn, girar180Btn;

    // Cropper
    private CropImageView imageToEdit;

    private RecyclerView recyclerViewImgs;

    // Datos
    private ImageAdapter adapter;
    private final List<Uri> listaFotosCamara = new ArrayList<>();
    private Uri currentSelectedUri = null;
    private int currentSelectedPosition = RecyclerView.NO_POSITION;

    // Claves
    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";

    // --- CICLO DE VIDA ---

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_cortar_rotar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Inicializar vistas de la barra superior
        regresarBtn = v.findViewById(R.id.regresar4);
        guardarBtn = v.findViewById(R.id.guardar);

        // Inicializar vistas de la barra de edición media
        girar90Btn = v.findViewById(R.id.deg90);
        girar180Btn = v.findViewById(R.id.deg180);
        cortarBtn = v.findViewById(R.id.cutt);

        // Inicializar vistas inferiores
        cam1 = v.findViewById(R.id.scancam1);
        eliminar1 = v.findViewById(R.id.eliminar1);

        imageToEdit = v.findViewById(R.id.imageToEdit);
        recyclerViewImgs = v.findViewById(R.id.imgsrecortar);

        recyclerViewImgs.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Configuración del CropImageView
        imageToEdit.setFixedAspectRatio(false);
        imageToEdit.setShowCropOverlay(true);
        imageToEdit.setAutoZoomEnabled(true);

        cargarFotosDesdeArgumentos();

        // Asignación de Click Listeners
        regresarBtn.setOnClickListener(this);
        guardarBtn.setOnClickListener(this);
        cam1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);

        // Listeners para los botones de edición
        girar90Btn.setOnClickListener(this);
        girar180Btn.setOnClickListener(this);
        cortarBtn.setOnClickListener(this);
    }

    // --- LÓGICA DE CARGA ---

    private void cargarFotosDesdeArgumentos() {
        listaFotosCamara.clear();
        Bundle args = getArguments();

        if (args != null) {
            ArrayList<String> uriStrings = args.getStringArrayList("FOTOS_CAPTURA");

            if (uriStrings != null) {
                for (String uriStr : uriStrings) {
                    try {
                        Uri fileUri = Uri.parse(uriStr);
                        listaFotosCamara.add(fileUri);
                    } catch (Exception e) {
                        Log.e("CortarRotar", "Error al parsear URI: " + uriStr, e);
                    }
                }
            }
        }

        adapter = new ImageAdapter(listaFotosCamara, this, R.layout.activity_item_imagen_delgado);
        recyclerViewImgs.setAdapter(adapter);

        if (!listaFotosCamara.isEmpty()) {
            onImageClicked(listaFotosCamara.get(0));
        }
    }

    // --- CALLBACKS DEL ADAPTADOR ---

    @Override
    public void onImageClicked(Uri uri) {
        currentSelectedUri = uri;
        currentSelectedPosition = listaFotosCamara.indexOf(uri);

        imageToEdit.setImageUriAsync(uri);

        if (adapter != null) adapter.setSelectedIndex(currentSelectedPosition);
    }

    @Override
    public void onSelectionChanged(int count) {
        // Implementación vacía
    }

    // --- MÉTODOS AUXILIARES ---

    private Uri guardarBitmapTemporalmente(Bitmap bitmap) {
        File cacheDir = requireContext().getCacheDir();
        // Usamos prefijo EDITADO_
        File file = new File(cacheDir, "EDITADO_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();

            return FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

        } catch (Exception e) {
            Log.e("CortarRotar", "Error guardando bitmap: " + e.getMessage());
            return null;
        }
    }

    private void procesarRecorteYGuardar() {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "No hay imagen seleccionada para guardar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardamos la URI del archivo original ANTES de reemplazarlo.
        Uri uriOriginal = listaFotosCamara.get(currentSelectedPosition);

        Bitmap bitmap = imageToEdit.getCroppedImage();
        if (bitmap != null) {
            Uri nuevaUri = guardarBitmapTemporalmente(bitmap); // Guarda el archivo EDITADO_...

            if (nuevaUri != null) {
                // 1. Reemplazar la URI antigua por la nueva URI editada
                listaFotosCamara.set(currentSelectedPosition, nuevaUri);

                // 2. Cargar la nueva imagen en el visor
                imageToEdit.setImageUriAsync(nuevaUri);

                // 3. Notificar el cambio al RecyclerView
                if (adapter != null) {
                    adapter.notifyItemChanged(currentSelectedPosition);
                }

                // 4. ELIMINAR EL ARCHIVO ORIGINAL
                try {
                    String fileNameOriginal = uriOriginal.getLastPathSegment();
                    if (fileNameOriginal != null) {
                        File fileOriginal = new File(requireContext().getCacheDir(), fileNameOriginal);
                        if (fileOriginal.exists()) {
                            fileOriginal.delete(); // Borra el archivo CAMARA_TEMP_...
                        }
                    }
                } catch (Exception e) {
                    Log.e("CortarRotar", "Advertencia: No se pudo eliminar el archivo original después de la edición.", e);
                }
                // *************************************************************

                Toast.makeText(requireContext(), "Cambios de recorte aplicados.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Error al guardar la imagen.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "No se pudo obtener el recorte. Intente rotar o recortar primero.", Toast.LENGTH_SHORT).show();
        }
    }


    private void regresarConResultado() {
        Bundle result = new Bundle();
        ArrayList<String> uris = new ArrayList<>();
        for (Uri uri : listaFotosCamara) uris.add(uri.toString());

        result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, uris);

        getParentFragmentManager().setFragmentResult(KEY_REORDENAR_RESULT, result);
        Navigation.findNavController(requireView()).popBackStack();
    }

    private void rotarImagen(int grados) {
        if (currentSelectedUri != null) {
            imageToEdit.rotateImage(grados);
            Toast.makeText(requireContext(), "Imagen rotada " + grados + "°.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Seleccione una imagen primero.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bundle crearBundleConUris() {
        Bundle bundle = new Bundle();
        ArrayList<String> uriStrings = new ArrayList<>();

        for (Uri uri : listaFotosCamara) {
            uriStrings.add(uri.toString());
        }

        bundle.putStringArrayList("FOTOS_CAPTURA", uriStrings);
        return bundle;
    }


    // --- MANEJO DE CLICKS ---

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if (id == R.id.regresar4 || id == R.id.guardar) {
            regresarConResultado();

        } else if (id == R.id.scancam1) {
            Bundle bundle = crearBundleConUris();
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3, bundle);


        } else if (id == R.id.eliminar1) {
            if (!listaFotosCamara.isEmpty()) {
                Bundle bundle = crearBundleConUris();
                Navigation.findNavController(v).navigate(R.id.escanerCaVerFotosTomadas, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para eliminar.", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.deg90) {
            rotarImagen(90);

        } else if (id == R.id.deg180) {
            rotarImagen(180);

        }  else if (id == R.id.cutt) {
            procesarRecorteYGuardar();

        }
    }
}