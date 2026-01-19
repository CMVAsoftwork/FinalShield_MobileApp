package com.example.finalshield.Fragments.Escaner;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.canhub.cropper.CropImageView;
import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CortarRotar extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    private static final int DESTINO_GALERIA_ID = R.id.seleccion_imagenes;
    private static final int DESTINO_CAMARA_ID = R.id.escanerCifradoMixto;

    ImageButton galeria, camara, edicion, eliminar;
    private Button regresar, deg90, deg180, cutt;
    private RecyclerView recycler;
    private SharedImageViewModel sharedViewModel;
    private CropImageView imageToEdit;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>();
    private Uri currentSelectedUri = null;
    private int currentSelectedPosition = RecyclerView.NO_POSITION;
    ImageAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cortar_rotar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // --- 1. Inicialización de Views ---
        regresar = v.findViewById(R.id.regresar1);

        deg90 = v.findViewById(R.id.deg90);
        deg180 = v.findViewById(R.id.deg180);
        cutt = v.findViewById(R.id.cutt);

        recycler = v.findViewById(R.id.imgsrecortar);
        imageToEdit = v.findViewById(R.id.imageToEdit);

        camara = v.findViewById(R.id.scancam);
        galeria = v.findViewById(R.id.selecgaleria);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);

        // --- 2. Listeners ---
        camara.setOnClickListener(this);
        galeria.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);

        regresar.setOnClickListener(this);

        deg90.setOnClickListener(view -> rotarImagen(90));
        deg180.setOnClickListener(view -> rotarImagen(180));

        cutt.setOnClickListener(view -> procesarRecorteYGuardar());

        imageToEdit.setFixedAspectRatio(false);
        imageToEdit.setShowCropOverlay(true);
        imageToEdit.setAutoZoomEnabled(true);

        cargarFotosDesdeViewModel();
    }

    // --- LÓGICA DE CARGA Y NAVEGACIÓN ---

    private void cargarFotosDesdeViewModel() {
        listaImagenesGaleria.clear();
        List<Uri> viewModelList = sharedViewModel.getImageUriList(); // Usa la lista combinada

        if (!viewModelList.isEmpty()) {
            listaImagenesGaleria.addAll(viewModelList);
        }

        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.activity_item_imagen_delgado, false);
        recycler.setAdapter(adapter);

        if (!listaImagenesGaleria.isEmpty()) {
            onImageClicked(listaImagenesGaleria.get(0));
        } else {
            Toast.makeText(getContext(), "No hay imágenes para recortar/rotar. Regresando a la cámara.", Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).navigate(DESTINO_CAMARA_ID);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // 🌟 Sincronizar el estado actual al ViewModel antes de navegar.
        sharedViewModel.setImageUriList(listaImagenesGaleria);

        if (id == R.id.scancam) {
            Navigation.findNavController(v).navigate(DESTINO_CAMARA_ID);
        } else if (id == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(DESTINO_GALERIA_ID);
        } else if (id == R.id.edicion) {
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (id == R.id.eliminar) {
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        } else if (id == R.id.regresar1) {
            Navigation.findNavController(v).popBackStack();
        }
    }

    private void guardarYSalir() {
        sharedViewModel.setImageUriList(listaImagenesGaleria);
        Toast.makeText(getContext(), "Cambios guardados y sincronizados. Regresando.", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }


    // --- LÓGICA DE EDICIÓN Y VISTA ---

    @Override
    public void onImageClicked(Uri uri) {
        currentSelectedUri = uri;
        currentSelectedPosition = listaImagenesGaleria.indexOf(uri);
        imageToEdit.setImageUriAsync(uri);
        if (adapter != null) adapter.setSelectedIndex(currentSelectedPosition);
    }

    @Override
    public void onSelectionChanged(int count) { /* No usado aquí */ }

    /**
     * 🌟 MODO DE ROTACIÓN CORREGIDO: Rotación manual del Bitmap subyacente.
     */
    private void rotarImagen(int grados) {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "Seleccione una imagen primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Rotar SOLO la vista CropImageView (feedback visual)
        imageToEdit.rotateImage(grados);

        File fileOriginal = getFileFromUri(currentSelectedUri);
        if (fileOriginal == null) {
            Toast.makeText(requireContext(), "Error: Archivo original no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Rotar el Bitmap subyacente COMPLETAMENTE (sin recorte)
        Bitmap rotatedBitmap = rotarBitmapManualmente(currentSelectedUri, grados);

        if (rotatedBitmap == null) {
            Toast.makeText(requireContext(), "Error al obtener o rotar la imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Guardar el Bitmap rotado sobre el archivo original (sobrescritura)
        Uri nuevaUri = guardarBitmapSobrescribiendo(rotatedBitmap, fileOriginal);
        if (rotatedBitmap != null) rotatedBitmap.recycle();

        // 4. Actualizar UI/ViewModel
        if (nuevaUri != null) {
            listaImagenesGaleria.set(currentSelectedPosition, nuevaUri);
            if (adapter != null) adapter.notifyItemChanged(currentSelectedPosition);
            sharedViewModel.setImageUriList(listaImagenesGaleria); // Sincronizar la lista combinada

            // Recargar la nueva URI rotada para resetear la rotación visual
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

        Bitmap bitmap = imageToEdit.getCroppedImage();

        if (bitmap != null) {
            Uri nuevaUri = guardarBitmapSobrescribiendo(bitmap, fileOriginal);
            if (bitmap != null) bitmap.recycle();

            if (nuevaUri != null) {
                listaImagenesGaleria.set(currentSelectedPosition, nuevaUri);
                if (adapter != null) adapter.notifyItemChanged(currentSelectedPosition);
                sharedViewModel.setImageUriList(listaImagenesGaleria); // Sincronizar la lista combinada

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


    // --- MÉTODOS AUXILIARES (COPIADOS) ---

    private File getFileFromUri(Uri fileProviderUri) {
        if (fileProviderUri == null || fileProviderUri.getPath() == null) return null;

        String filePath = fileProviderUri.getPath();
        String fileName = new File(filePath).getName();

        File cacheDir = requireContext().getCacheDir();
        File targetFile = new File(cacheDir, fileName);

        if (targetFile.exists()) {
            return targetFile;
        }
        return null;
    }

    private Bitmap rotarBitmapManualmente(Uri uri, int grados) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            if (originalBitmap == null) return null;

            Matrix matrix = new Matrix();
            matrix.postRotate(grados);

            Bitmap rotatedBitmap = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.getWidth(),
                    originalBitmap.getHeight(), matrix, true
            );
            originalBitmap.recycle();
            return rotatedBitmap;
        } catch (Exception e) {
            Log.e("CortarRotar", "Error al rotar bitmap manualmente: " + e.getMessage(), e);
            return null;
        }
    }

    private Uri guardarBitmapSobrescribiendo(Bitmap bitmap, File fileOriginal) {
        if (fileOriginal == null || !fileOriginal.exists()) {
            Log.e("CortarRotar", "El archivo original para sobrescribir no es válido.");
            return null;
        }

        try (FileOutputStream out = new FileOutputStream(fileOriginal)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();

            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", fileOriginal);
        } catch (Exception e) {
            Log.e("CortarRotar", "Error guardando bitmap: " + e.getMessage(), e);
            return null;
        }
    }
}