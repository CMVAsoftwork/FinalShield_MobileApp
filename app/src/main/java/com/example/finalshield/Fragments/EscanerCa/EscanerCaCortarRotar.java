package com.example.finalshield.Fragments.EscanerCa;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import android.widget.Toast;

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

        // Inicialización de Vistas (código omitido para brevedad, asumo que es correcto)
        regresarBtn = v.findViewById(R.id.regresar4);
        guardarBtn = v.findViewById(R.id.guardar);
        girar90Btn = v.findViewById(R.id.deg90);
        girar180Btn = v.findViewById(R.id.deg180);
        cortarBtn = v.findViewById(R.id.cutt);
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

        // Asignación de Click Listeners (código omitido para brevedad, asumo que es correcto)
        regresarBtn.setOnClickListener(this);
        guardarBtn.setOnClickListener(this);
        cam1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);
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

        // Importante: Reinicia el CropImageView con la imagen original
        imageToEdit.setImageUriAsync(uri);

        if (adapter != null) adapter.setSelectedIndex(currentSelectedPosition);
    }

    @Override
    public void onSelectionChanged(int count) {
        // Implementación vacía
    }

    // --- MÉTODOS AUXILIARES Y LÓGICA DE EDICIÓN CORREGIDA ---

    /**
     * Resuelve un Uri de FileProvider a un objeto File buscando en la caché.
     */
    private File getFileFromUri(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName == null) return null;

        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();

        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isFile() && file.getName().equals(fileName)) {
                    return file;
                }
            }
        }
        return null;
    }

    /**
     * Guarda el Bitmap sobrescribiendo el archivo físico original, manteniendo la misma URI.
     * @param bitmap El Bitmap editado (recorte o rotación).
     * @param fileOriginal El File original que debe ser sobrescrito.
     * @return La URI del archivo guardado (la misma que la original).
     */
    private Uri guardarBitmapSobrescribiendo(Bitmap bitmap, File fileOriginal) {

        if (fileOriginal == null || !fileOriginal.exists()) {
            Log.e("CortarRotar", "El archivo original para sobrescribir no es válido.");
            return null;
        }

        File fileToSave = fileOriginal;

        try (FileOutputStream out = new FileOutputStream(fileToSave)) {
            // Comprimir el bitmap y escribirlo en el archivo
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();

            // Devolvemos la URI del archivo guardado
            return FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    fileToSave
            );

        } catch (Exception e) {
            Log.e("CortarRotar", "Error guardando bitmap: " + e.getMessage());
            return null;
        }
    }

    private void rotarImagen(int grados) {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "Seleccione una imagen primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Aplicar la rotación visual en el CropImageView
        imageToEdit.rotateImage(grados);

        // 2. Obtener la imagen rotada (Bitmap)
        // CropImageView.getCroppedImage() obtiene el bitmap con la rotación actual aplicada.
        Bitmap rotatedBitmap = imageToEdit.getCroppedImage();

        if (rotatedBitmap == null) {
            Toast.makeText(requireContext(), "Error al obtener la imagen rotada.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Obtener el archivo original para sobrescribir
        File fileOriginal = getFileFromUri(currentSelectedUri);

        if (fileOriginal == null) {
            Toast.makeText(requireContext(), "Error: Archivo original no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Guardar sobrescribiendo el archivo original
        Uri nuevaUri = guardarBitmapSobrescribiendo(rotatedBitmap, fileOriginal);

        if (nuevaUri != null) {
            // 5. Refrescar la vista y la miniatura
            imageToEdit.setImageUriAsync(nuevaUri);

            if (adapter != null) {
                adapter.notifyItemChanged(currentSelectedPosition);
            }

            Toast.makeText(requireContext(), "Imagen rotada y actualizada.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Error al guardar la rotación.", Toast.LENGTH_SHORT).show();
        }
    }


    private void procesarRecorteYGuardar() {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "No hay imagen seleccionada para recortar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el File del archivo actual a partir de la URI seleccionada
        File fileOriginal = getFileFromUri(currentSelectedUri);

        if (fileOriginal == null) {
            Toast.makeText(requireContext(), "Error: No se encontró el archivo original para el recorte.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener el bitmap recortado (esto incluye rotaciones si se aplicaron antes)
        Bitmap bitmap = imageToEdit.getCroppedImage();

        if (bitmap != null) {
            // Guardar sobrescribiendo el archivo original
            Uri nuevaUri = guardarBitmapSobrescribiendo(bitmap, fileOriginal);

            if (nuevaUri != null) {
                // Recargar la imagen y refrescar la miniatura
                imageToEdit.setImageUriAsync(nuevaUri);
                if (adapter != null) {
                    adapter.notifyItemChanged(currentSelectedPosition);
                }

                Toast.makeText(requireContext(), "Recorte aplicado y guardado.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Error al guardar el recorte.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "No se pudo obtener el recorte. Intente de nuevo.", Toast.LENGTH_SHORT).show();
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
            // Rotar 90 grados y guardar
            rotarImagen(90);

        } else if (id == R.id.deg180) {
            // Rotar 180 grados y guardar
            rotarImagen(180);

        }  else if (id == R.id.cutt) {
            // Aplicar recorte y guardar
            procesarRecorteYGuardar();

        }
    }
}