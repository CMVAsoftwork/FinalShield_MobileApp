package com.example.finalshield.Fragments.EscanerGa;

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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EscanerCifradoGaleria extends Fragment implements View.OnClickListener {

    public static final String KEY_GUARDAR_SELECCION = "guardar_seleccion_key";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "EscanerGaleria";

    private SharedImageViewModel sharedViewModel;

    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenes2 = new ArrayList<>(); // Lista de la galería completa

    // Lista MAESTRA: Contiene las URIs de FileProvider que se han seleccionado/consolidado
    private final List<Uri> listaSeleccionadaParaGuardar = new ArrayList<>();

    private TextView selectionCount;
    private Button cancelarSeleccion;
    private Button guardarFotosSeleccionadas;
    private LinearLayout selectionBar;
    ImageButton recortar2, edicion2, eliminar2;
    private Button regresar;
    private Button guardar;

    private final ActivityResultLauncher<Intent> vistaImagenLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Callback para cuando se regresa de VistaImagenActivity
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);

        // ✅ RECEPTOR DEL RESULTADO DE ELIMINACIÓN/EDICIÓN
        getParentFragmentManager().setFragmentResultListener(
                EscanerGaEliminarPaginas.KEY_ACTUALIZACION_LISTA,
                this,
                new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        if (requestKey.equals(EscanerGaEliminarPaginas.KEY_ACTUALIZACION_LISTA)) {
                            ArrayList<String> updatedUriStrings =
                                    result.getStringArrayList(EscanerGaEliminarPaginas.BUNDLE_RESULT_URI_LIST);

                            if (updatedUriStrings != null) {
                                actualizarListaDespuesDeEliminacion(updatedUriStrings);
                            }
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_galeria, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recyclerViee = v.findViewById(R.id.recycler2);
        recyclerViee.setLayoutManager(new GridLayoutManager(getContext(), 2));

        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        cancelarSeleccion = v.findViewById(R.id.clearSelection);
        guardarFotosSeleccionadas = v.findViewById(R.id.guardarSeleccion);

        recortar2 = v.findViewById(R.id.recortar2);
        edicion2 = v.findViewById(R.id.edicion2);
        eliminar2 = v.findViewById(R.id.eliminar2);
        regresar = v.findViewById(R.id.regresar3);
        guardar = v.findViewById(R.id.guardar3);

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

        recortar2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        // ✅ CARGA INICIAL DESDE VIEWMODEL (Reflejar estado al volver)
        listaSeleccionadaParaGuardar.clear();
        List<Uri> viewModelList = sharedViewModel.getImageUriList();
        if (!viewModelList.isEmpty()) {
            listaSeleccionadaParaGuardar.addAll(viewModelList);
        }

        pedirPermiso();
    }

    // --- MANEJO DE PERMISOS Y CARGA DE GALERÍA (Sin cambios) ---

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
        listaImagenes2.clear();
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

        listaImagenes2.addAll(loadedUris);

        if (listaImagenes2.isEmpty()) {
            Toast.makeText(getContext(), "No se encontraron imágenes en la galería.", Toast.LENGTH_LONG).show();
        }

        adapter = new ImageAdapter(
                listaImagenes2,
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

    /**
     * Procesa el resultado de eliminación y actualiza la lista maestra.
     */
    private void actualizarListaDespuesDeEliminacion(ArrayList<String> updatedUriStrings) {

        List<Uri> retainedUris = new ArrayList<>();
        for (String uriStr : updatedUriStrings) {
            retainedUris.add(Uri.parse(uriStr));
        }

        int initialMasterSize = listaSeleccionadaParaGuardar.size();

        // 1. Reemplazar la lista maestra con el resultado actualizado
        listaSeleccionadaParaGuardar.clear();
        listaSeleccionadaParaGuardar.addAll(retainedUris);

        // 2. Actualizar el ViewModel
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);

        int finalSize = listaSeleccionadaParaGuardar.size();
        int deletedFromMaster = initialMasterSize - finalSize;

        if (finalSize == 0) {
            Toast.makeText(getContext(), "Se eliminaron todos los elementos. La lista de trabajo está vacía.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), deletedFromMaster + " imágenes eliminadas/editadas. Total: " + finalSize, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Guarda las imágenes seleccionadas, creando una copia única para edición.
     */
    private void guardarImagenesSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> selectedUris = adapter.getSelectedItems(); // URIs de la Galería
        int countAdded = 0;

        // 1. Obtener los identificadores únicos (IDs) de las URIs de la Galería que ya están en la lista MAESTRA
        Set<String> originalIdInMaster = getOriginalIdInMaster(listaSeleccionadaParaGuardar);

        // 2. Iterar sobre las selecciones de la Galería
        for (Uri galleryUri : selectedUris) {
            String galleryId = galleryUri.getLastPathSegment();

            // Comprobar duplicado por ID original (para que no se añada la misma imagen de la galería dos veces)
            if (!originalIdInMaster.contains(galleryId)) {

                // Si es un archivo nuevo, creamos una copia editable ÚNICA
                Uri localEditableUri = obtenerCopiaEditableUnica(galleryUri);

                if (localEditableUri != null) {
                    listaSeleccionadaParaGuardar.add(localEditableUri);
                    originalIdInMaster.add(galleryId);
                    countAdded++;
                }
            }
        }

        // Almacenar la lista final consolidada en el ViewModel
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);

        // Notificar al fragmento padre sobre la selección (opcional, dependiendo del flujo)
        Bundle result = new Bundle();
        ArrayList<String> selectedUrisStr = new ArrayList<>();
        for (Uri uri : listaSeleccionadaParaGuardar) selectedUrisStr.add(uri.toString());

        result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, selectedUrisStr);
        getParentFragmentManager().setFragmentResult(KEY_GUARDAR_SELECCION, result);

        // Limpiar la selección visual después de guardar
        adapter.clearSelection();
        actualizarBarra();

        Toast.makeText(getContext(), countAdded + " nuevas fotos añadidas para guardar. Total: " + listaSeleccionadaParaGuardar.size(), Toast.LENGTH_LONG).show();
    }

    /**
     * Auxiliar: Obtiene los IDs de la Galería a partir de las URIs de FileProvider en la lista MAESTRA.
     */
    private Set<String> getOriginalIdInMaster(List<Uri> masterList) {
        Set<String> ids = new HashSet<>();
        for (Uri fileProviderUri : masterList) {
            // El formato es "editable_<original_segment>_<uuid>.jpg"
            String path = fileProviderUri.getLastPathSegment();
            if (path != null) {
                // Buscamos el segmento entre "editable_" y "_<uuid>"
                int start = path.indexOf('_');
                int end = path.lastIndexOf('_');
                if (start != -1 && end > start) {
                    ids.add(path.substring(start + 1, end));
                }
            }
        }
        return ids;
    }

    /**
     * Auxiliar: Crea una copia editable única en el caché.
     */
    private Uri obtenerCopiaEditableUnica(Uri galleryUri) {
        try {
            // Usamos un UUID para asegurar la unicidad del archivo
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String originalSegment = galleryUri.getLastPathSegment();

            // Formato: editable_<original_segment>_<uniqueId>.jpg
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

        // ✅ CLAVE: Asegurarse de que el ViewModel esté sincronizado ANTES de navegar
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);

        if (id == R.id.recortar2) {
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para recortar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        } else if (id == R.id.edicion2) {
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para editar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);
            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        } else if (id == R.id.eliminar2) {
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para eliminar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);

            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        } else if (id == R.id.regresar3) {
            listaSeleccionadaParaGuardar.clear();
            sharedViewModel.clearList();
            Toast.makeText(getContext(), "Selección de trabajo borrada. Regresando.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (id == R.id.guardar3) {
            Toast.makeText(getContext(), "Guardando " + listaSeleccionadaParaGuardar.size() + " imágenes...", Toast.LENGTH_SHORT).show();
            // Implementa aquí la lógica final de guardado cifrado
        }
    }
}