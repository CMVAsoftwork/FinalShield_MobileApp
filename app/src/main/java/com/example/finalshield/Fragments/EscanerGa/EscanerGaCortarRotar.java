package com.example.finalshield.Fragments.EscanerGa;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.canhub.cropper.CropImageView;
import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EscanerGaCortarRotar extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    // Variables declaradas:
    ImageButton selecimg2, recortar2, edicion2, eliminar2;
    private Button regresar, deg90, deg180, cutt; // IDs del XML: regresar3, guardar, deg90, deg180, cutt

    private RecyclerView recycler;
    private SharedImageViewModel sharedViewModel;
    private CropImageView imageToEdit;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>(); // Contiene URIs de FileProvider (editables)
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
        return inflater.inflate(R.layout.fragment_escaner_ga_cortar_rotar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // --- 1. Inicialización de Views ---
        regresar = v.findViewById(R.id.regresar3);

        deg90 = v.findViewById(R.id.deg90);
        deg180 = v.findViewById(R.id.deg180);
        cutt = v.findViewById(R.id.cutt);

        recycler = v.findViewById(R.id.imgsrecortarG);
        imageToEdit = v.findViewById(R.id.imageToEdit);

        selecimg2 = v.findViewById(R.id.selecgaleria2);
        recortar2 = v.findViewById(R.id.recortar2);
        edicion2 = v.findViewById(R.id.edicion2);
        eliminar2 = v.findViewById(R.id.eliminar2);

        // --- 2. Listeners ---
        selecimg2.setOnClickListener(this);
        recortar2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);

        regresar.setOnClickListener(this);

        deg90.setOnClickListener(view -> rotarImagen(90));
        deg180.setOnClickListener(view -> rotarImagen(180));
        cutt.setOnClickListener(view -> procesarRecorteYGuardar());

        imageToEdit.setFixedAspectRatio(false);
        imageToEdit.setShowCropOverlay(true);
        imageToEdit.setAutoZoomEnabled(true);

        cargarFotosDesdeViewModel();
    }

    // --- LÓGICA DE CARGA Y COPIA DE ARCHIVOS ---

    private void cargarFotosDesdeViewModel() {
        listaImagenesGaleria.clear();
        List<Uri> viewModelList = sharedViewModel.getImageUriList(); // Contiene las URIs de FileProvider (editables)

        // Aquí solo cargamos la lista del ViewModel, ya que EscanerCifradoGaleria
        // ya se encargó de copiar las URIs de la Galería a FileProvider URIs y de asegurar la unicidad.
        if (!viewModelList.isEmpty()) {
            listaImagenesGaleria.addAll(viewModelList);
        }

        recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.activity_item_imagen_delgado, false);
        recycler.setAdapter(adapter);

        if (!listaImagenesGaleria.isEmpty()) {
            onImageClicked(listaImagenesGaleria.get(0));
        } else {
            Toast.makeText(getContext(), "No hay imágenes para recortar/rotar.", Toast.LENGTH_LONG).show();
        }
    }

    // --- LÓGICA DE NAVEGACIÓN Y SALIDA ---

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // CLAVE: Actualizar el ViewModel antes de CUALQUIER navegación.
        sharedViewModel.setImageUriList(listaImagenesGaleria);

        if (id == R.id.selecgaleria2) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoGaleria2);
        } else if (id == R.id.recortar2) {
            Toast.makeText(getContext(), "Ya estás en la herramienta de recorte.", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.edicion2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);
        } else if (id == R.id.eliminar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);
        } else if (id == R.id.regresar3) {
            guardarYSalir();
        }
    }

    private void guardarYSalir() {
        sharedViewModel.setImageUriList(listaImagenesGaleria);
        Toast.makeText(getContext(), "Guardando cambios y regresando.", Toast.LENGTH_SHORT).show();
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

    private void rotarImagen(int grados) {
        if (currentSelectedUri == null || currentSelectedPosition == RecyclerView.NO_POSITION) {
            Toast.makeText(requireContext(), "Seleccione una imagen primero.", Toast.LENGTH_SHORT).show();
            return;
        }

        imageToEdit.rotateImage(grados);

        File fileOriginal = getFileFromUri(currentSelectedUri);
        if (fileOriginal == null) {
            Toast.makeText(requireContext(), "Error: Archivo original no encontrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap rotatedBitmap = rotarBitmapManualmente(currentSelectedUri, grados);

        if (rotatedBitmap == null) {
            Toast.makeText(requireContext(), "Error al obtener o rotar la imagen.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri nuevaUri = guardarBitmapSobrescribiendo(rotatedBitmap, fileOriginal);
        rotatedBitmap.recycle();

        if (nuevaUri != null) {
            listaImagenesGaleria.set(currentSelectedPosition, nuevaUri);
            if (adapter != null) adapter.notifyItemChanged(currentSelectedPosition);
            sharedViewModel.setImageUriList(listaImagenesGaleria);

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

            if (nuevaUri != null) {
                listaImagenesGaleria.set(currentSelectedPosition, nuevaUri);
                if (adapter != null) adapter.notifyItemChanged(currentSelectedPosition);
                sharedViewModel.setImageUriList(listaImagenesGaleria);

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

    // --- MÉTODOS AUXILIARES (Funcionales y necesarios) ---

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
            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
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