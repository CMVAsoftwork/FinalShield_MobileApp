package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
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

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.Fragments.EscanerGa.EscanerGaEliminarPaginas;
import com.example.finalshield.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Seleccion_imagenes extends Fragment implements View.OnClickListener {
    ImageButton camara,recortar, edicion, eliminar;
    private Button regresar;
    private Button guardar;

    private static final int REQUEST_CODE = 100;
    private static final String TAG = "EscanerGaleria";

    private SharedImageViewModel sharedViewModel;

    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaMixta = new ArrayList<>(); // Lista de la galería completa

    private TextView selectionCount;
    private Button cancelarSeleccion;
    private Button guardarFotosSeleccionadas;
    private LinearLayout selectionBar;

    private final ActivityResultLauncher<Intent> vistaImagenLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> { /* ... */ });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Asegúrate de reemplazar "R.layout.fragment_seleccion_imagenes" con el layout correcto.
        return inflater.inflate(R.layout.fragment_seleccion_imagenes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recyclerViee = v.findViewById(R.id.recycler);
        recyclerViee.setLayoutManager(new GridLayoutManager(getContext(), 2));

        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        cancelarSeleccion = v.findViewById(R.id.clearSelection);
        guardarFotosSeleccionadas = v.findViewById(R.id.guardarSeleccion);

        recortar = v.findViewById(R.id.recortar);
        camara = v.findViewById(R.id.scann);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);
        regresar = v.findViewById(R.id.regresar);
        guardar = v.findViewById(R.id.guardar);

        cancelarSeleccion.setOnClickListener(view -> {
            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        });

        guardarFotosSeleccionadas.setOnClickListener(view -> {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                guardarImagenesSeleccionadas();
            } else {
                Toast.makeText(getContext(), "No hay fotos seleccionadas para guardar.", Toast.LENGTH_SHORT).show();
            }
        });

        recortar.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);
        camara.setOnClickListener(this);

        pedirPermiso();
    }

    // --- MANEJO DE PERMISOS Y CARGA DE GALERÍA (Sin cambios) ---
    // (Métodos pedirPermiso, onRequestPermissionsResult, cargarImagenes son iguales)

    // ... (Inserte aquí los métodos de permisos y carga de imágenes) ...
    private void pedirPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE);
        else
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cargarImagenes();
        } else {
            Toast.makeText(getContext(), "Permiso requerido para acceder a la galería.", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarImagenes() {
        listaMixta.clear();
        List<Uri> loadedUris = new ArrayList<>();

        Uri collection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        try (Cursor cursor = requireActivity().getContentResolver().query(
                collection,
                new String[]{MediaStore.Images.Media._ID},
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                if (idColumn != -1) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        Uri uri = ContentUris.withAppendedId(collection, id);
                        loadedUris.add(uri);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al consultar MediaStore: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error al consultar MediaStore: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        listaMixta.addAll(loadedUris);

        if (listaMixta.isEmpty()) {
            Toast.makeText(getContext(), "No se encontraron imágenes en la galería.", Toast.LENGTH_LONG).show();
        }

        adapter = new ImageAdapter(
                listaMixta,
                new ImageAdapter.Callbacks() {
                    @Override
                    public void onImageClicked(Uri uri) { /* ... */ }
                    @Override
                    public void onSelectionChanged(int count) { actualizarBarra(); }
                },
                R.layout.item_imagen,
                true
        );

        recyclerViee.setAdapter(adapter);
    }
    // --- LÓGICA DE ACTUALIZACIÓN Y GUARDADO ---

    private void guardarImagenesSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> selectedUris = adapter.getSelectedItems();
        List<Uri> listaMaestraActual = new ArrayList<>(sharedViewModel.getImageUriList());
        int countAdded = 0;

        Set<String> originalIdInMaster = getOriginalIdInMaster(listaMaestraActual);

        for (Uri galleryUri : selectedUris) {
            String galleryId = galleryUri.getLastPathSegment();

            if (galleryId != null && !originalIdInMaster.contains(galleryId)) {

                Uri localEditableUri = obtenerCopiaEditableUnica(galleryUri);

                if (localEditableUri != null) {
                    listaMaestraActual.add(localEditableUri);
                    originalIdInMaster.add(galleryId);
                    countAdded++;
                }
            }
        }

        sharedViewModel.setImageUriList(listaMaestraActual);

        adapter.clearSelection();
        actualizarBarra();

        Toast.makeText(getContext(), countAdded + " nuevas fotos añadidas para guardar. Total: " + listaMaestraActual.size(), Toast.LENGTH_LONG).show();
    }

    private Set<String> getOriginalIdInMaster(List<Uri> masterList) {
        Set<String> ids = new HashSet<>();
        for (Uri fileProviderUri : masterList) {
            String path = fileProviderUri.getLastPathSegment();
            if (path != null && path.startsWith("editable_")) {
                int start = path.indexOf('_');
                int end = path.lastIndexOf('_');
                if (start != -1 && end > start) {
                    ids.add(path.substring(start + 1, end));
                }
            }
        }
        return ids;
    }

    private Uri obtenerCopiaEditableUnica(Uri galleryUri) {
        try {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String originalSegment = galleryUri.getLastPathSegment();

            String filename = "editable_" + originalSegment + "_" + uniqueId + ".jpg";
            File file = new File(requireContext().getCacheDir(), filename);

            InputStream inputStream = requireContext().getContentResolver().openInputStream(galleryUri);
            if (inputStream == null) return null;

            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);

        } catch (Exception e) {
            Log.e(TAG, "Fallo al crear copia editable única: " + e.getMessage(), e);
            return null;
        }
    }

    private void actualizarBarra() {
        if (adapter == null) return;

        int count = adapter.getSelectedCount();

        if (count == 0) {
            selectionBar.setVisibility(View.GONE);
        } else {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText(count + " seleccionadas");
            guardarFotosSeleccionadas.setEnabled(count > 0);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        List<Uri> listaActual = sharedViewModel.getImageUriList();

        if(id == R.id.scann){
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        } else if (id == R.id.recortar) {
            if (listaActual.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para recortar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        } else if (id == R.id.edicion) {
            if (listaActual.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para acomodar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        } else if (id == R.id.eliminar) {
            if (listaActual.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para eliminar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);

            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        } else if (id == R.id.regresar) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        }
    }
}