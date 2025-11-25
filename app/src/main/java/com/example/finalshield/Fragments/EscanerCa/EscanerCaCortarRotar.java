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

    // ... (Variables de instancia como las tenías)
    ImageButton cam1, eliminar1;
    private Button regresarBtn, guardarBtn;
    private Button cortarBtn, girar90Btn, girar180Btn;

    private CropImageView imageToEdit;
    private RecyclerView recyclerViewImgs;

    private ImageAdapter adapter;
    private final List<Uri> listaFotosCamara = new ArrayList<>();
    private Uri currentSelectedUri = null;
    private int currentSelectedPosition = RecyclerView.NO_POSITION;

    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";


    // ---------------------------------------------------------------------------------
    // Ciclo de Vida del Fragment
    // ---------------------------------------------------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_cortar_rotar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Inicialización de Vistas
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

        // Configuración de CropImageView
        imageToEdit.setFixedAspectRatio(false);
        imageToEdit.setShowCropOverlay(true);
        imageToEdit.setAutoZoomEnabled(true);

        cargarFotosDesdeArgumentos();

        // Listeners
        regresarBtn.setOnClickListener(this);
        guardarBtn.setOnClickListener(this);
        cam1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);
        girar90Btn.setOnClickListener(this);
        girar180Btn.setOnClickListener(this);
        cortarBtn.setOnClickListener(this);
    }

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

        if (!listaFotosCamara.isEmpty()) onImageClicked(listaFotosCamara.get(0));
    }


    // ---------------------------------------------------------------------------------
    // Interfaz ImageAdapter.Callbacks
    // ---------------------------------------------------------------------------------

    @Override
    public void onImageClicked(Uri uri) {
        currentSelectedUri = uri;
        currentSelectedPosition = listaFotosCamara.indexOf(uri);

        // Carga la imagen seleccionada en el editor
        imageToEdit.setImageUriAsync(uri);

        if (adapter != null) adapter.setSelectedIndex(currentSelectedPosition);
    }

    @Override
    public void onSelectionChanged(int count) { /* no usado */ }


    // ---------------------------------------------------------------------------------
    // Métodos Auxiliares de Archivos
    // ---------------------------------------------------------------------------------

    /**
     * Intenta obtener el objeto File del archivo al que apunta la URI.
     * Esto asume que las URIs son creadas por FileProvider y están en la caché.
     */
    private File getFileFromUri(Uri uri) {
        String fileName = (uri == null) ? null : uri.getLastPathSegment();
        if (fileName == null) return null;

        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                // Buscamos un archivo en el caché que termine con el mismo nombre que el path segment de la URI
                // Nota: Esto puede ser simplificado si solo guardas el File en lugar de la URI en la lista.
                // Pero manteniendo tu lógica:
                if (file.isFile() && file.getName().equals(fileName)) return file;
            }
        }
        return null;
    }

    /**
     * Guarda el Bitmap sobrescribiendo el archivo original y devuelve la URI.
     */
    private Uri guardarBitmapSobrescribiendo(Bitmap bitmap, File fileOriginal) {
        if (fileOriginal == null || !fileOriginal.exists()) {
            Log.e("CortarRotar", "El archivo original para sobrescribir no es válido.");
            return null;
        }

        try (FileOutputStream out = new FileOutputStream(fileOriginal)) {
            // Guarda el bitmap en el archivo, sobrescribiendo el contenido anterior
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();

            // Vuelve a obtener la URI para garantizar que es correcta (aunque el File es el mismo)
            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", fileOriginal);
        } catch (Exception e) {
            Log.e("CortarRotar", "Error guardando bitmap: " + e.getMessage(), e);
            return null;
        }
    }


    // ---------------------------------------------------------------------------------
    // Lógica de Rotación y Recorte (CORREGIDA)
    // ---------------------------------------------------------------------------------

    private void rotarImagen(int grados) {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "Seleccione una imagen primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Aplica la rotación visualmente en el editor
        imageToEdit.rotateImage(grados);

        // 2. Obtiene la imagen rotada como Bitmap
        Bitmap rotatedBitmap = imageToEdit.getCroppedImage();
        if (rotatedBitmap == null) {
            Toast.makeText(requireContext(), "Error al obtener la imagen rotada.", Toast.LENGTH_SHORT).show();
            return;
        }

        File fileOriginal = getFileFromUri(currentSelectedUri);
        if (fileOriginal == null) {
            Toast.makeText(requireContext(), "Error: Archivo original no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Guarda el nuevo Bitmap sobrescribiendo el archivo original
        Uri nuevaUri = guardarBitmapSobrescribiendo(rotatedBitmap, fileOriginal);

        if (nuevaUri != null) {
            // 4. Actualiza la lista y notifica al RecyclerView
            listaFotosCamara.set(currentSelectedPosition, nuevaUri);
            if (adapter != null) adapter.notifyItemChanged(currentSelectedPosition);

            // 5. Vuelve a cargar la imagen en el editor desde la URI para reflejar el cambio y restablecer el zoom/pan
            imageToEdit.setImageUriAsync(nuevaUri);
            currentSelectedUri = nuevaUri;
            Toast.makeText(requireContext(), "Imagen rotada y guardada.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Error al guardar la rotación.", Toast.LENGTH_SHORT).show();
        }
    }

    private void procesarRecorteYGuardar() {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "No hay imagen seleccionada para recortar.", Toast.LENGTH_SHORT).show();
            return;
        }

        File fileOriginal = getFileFromUri(currentSelectedUri);
        if (fileOriginal == null) {
            Toast.makeText(requireContext(), "Error: No se encontró el archivo original para el recorte.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Obtiene la imagen recortada como Bitmap
        Bitmap bitmap = imageToEdit.getCroppedImage();
        if (bitmap != null) {
            // 2. Guarda el nuevo Bitmap sobrescribiendo el archivo original
            Uri nuevaUri = guardarBitmapSobrescribiendo(bitmap, fileOriginal);

            if (nuevaUri != null) {
                // 3. Actualiza la lista y notifica al RecyclerView
                listaFotosCamara.set(currentSelectedPosition, nuevaUri);
                if (adapter != null) adapter.notifyItemChanged(currentSelectedPosition);

                // 4. Vuelve a cargar la imagen en el editor desde la URI para reflejar el cambio y restablecer el zoom/pan
                imageToEdit.setImageUriAsync(nuevaUri);
                currentSelectedUri = nuevaUri;
                Toast.makeText(requireContext(), "Recorte aplicado y guardado.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Error al guardar el recorte.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "No se pudo obtener el recorte. Intente de nuevo.", Toast.LENGTH_SHORT).show();
        }
    }


    // ---------------------------------------------------------------------------------
    // Lógica de Navegación y Resultados
    // ---------------------------------------------------------------------------------

    /**
     * Prepara y envía la lista de URIs actualizada al Fragment anterior.
     */
    private void regresarConResultado() {
        Bundle result = new Bundle();
        ArrayList<String> uris = new ArrayList<>();
        // Usa la lista actualizada
        for (Uri uri : listaFotosCamara) uris.add(uri.toString());
        result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, uris);

        // Establece el resultado para que el Fragment de destino lo reciba
        getParentFragmentManager().setFragmentResult(KEY_REORDENAR_RESULT, result);
        Navigation.findNavController(requireView()).popBackStack();
    }

    private Bundle crearBundleConUris() {
        Bundle bundle = new Bundle();
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : listaFotosCamara) uriStrings.add(uri.toString());
        bundle.putStringArrayList("FOTOS_CAPTURA", uriStrings);
        return bundle;
    }


    // ---------------------------------------------------------------------------------
    // Manejo de Clicks
    // ---------------------------------------------------------------------------------

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.regresar4 || id == R.id.guardar) {
            // REGRESAR O GUARDAR: Manda la lista con los cambios
            regresarConResultado();
        } else if (id == R.id.scancam1) {
            // CÁMARA: Navega a la cámara con la lista actual
            Bundle bundle = crearBundleConUris();
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3, bundle);
        } else if (id == R.id.eliminar1) {
            // ELIMINAR: Navega a la vista de fotos para eliminar (con la lista actual)
            if (!listaFotosCamara.isEmpty()) {
                Bundle bundle = crearBundleConUris();
                Navigation.findNavController(v).navigate(R.id.escanerCaVerFotosTomadas, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para eliminar.", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.deg90) {
            // ROTAR 90
            rotarImagen(90);
        } else if (id == R.id.deg180) {
            // ROTAR 180
            rotarImagen(180);
        } else if (id == R.id.cutt) {
            // RECORTAR
            procesarRecorteYGuardar();
        }
    }
}