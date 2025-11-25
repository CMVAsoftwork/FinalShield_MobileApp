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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EscanerCifradoGaleria extends Fragment implements View.OnClickListener {

    public static final String KEY_GUARDAR_SELECCION = "guardar_seleccion_key";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";
    private static final int REQUEST_CODE = 100;

    private SharedImageViewModel sharedViewModel;

    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenes2 = new ArrayList<>(); // Lista de la galería completa

    // Lista MAESTRA: Contiene las URIs que el usuario ha seleccionado/consolidado para trabajar
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

        // Listener para recibir la lista actualizada después de la eliminación
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

        // CORRECCIÓN DE FLUJO: Inicializa la listaSeleccionadaParaGuardar desde el ViewModel al iniciar
        List<Uri> viewModelList = sharedViewModel.getImageUriList();
        if (!viewModelList.isEmpty()) {
            listaSeleccionadaParaGuardar.addAll(viewModelList);
        }
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

        pedirPermiso();
    }

    // ... (pedirPermiso y onRequestPermissionsResult sin cambios)

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
        // Lógica de MediaStore para cargar listaImagenes2 (galería completa)
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

    /**
     * Procesa el resultado de eliminación.
     * CORRECCIÓN: Refleja los cambios en la lista MAESTRA (listaSeleccionadaParaGuardar)
     * Y en la lista de la galería (listaImagenes2).
     */
    private void actualizarListaDespuesDeEliminacion(ArrayList<String> updatedUriStrings) {

        List<Uri> retainedUris = new ArrayList<>();
        for (String uriStr : updatedUriStrings) {
            retainedUris.add(Uri.parse(uriStr));
        }

        // 1. **ACTUALIZAR LISTA MAESTRA**
        // La lista devuelta por el fragmento de eliminación es la lista MAESTRA sin los ítems eliminados.
        int initialMasterSize = listaSeleccionadaParaGuardar.size();
        listaSeleccionadaParaGuardar.clear();
        listaSeleccionadaParaGuardar.addAll(retainedUris);
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar); // Actualizar ViewModel

        int deletedFromMaster = initialMasterSize - listaSeleccionadaParaGuardar.size();

        // 2. **ACTUALIZAR LISTA DE GALERÍA** (listaImagenes2)
        // Esto es necesario si EscanerGaEliminarPaginas elimina permanentemente archivos de la galería.
        // Si EscanerGaEliminarPaginas SOLO elimina ítems de la lista MAESTRA (listaSeleccionadaParaGuardar),
        // este paso no es necesario. Basado en el flujo, asumo que SÓLO elimina de la lista MAESTRA.

        Toast.makeText(getContext(), deletedFromMaster + " imágenes eliminadas de la lista de trabajo. Total: " + listaSeleccionadaParaGuardar.size(), Toast.LENGTH_LONG).show();
    }


    /**
     * Guarda las imágenes seleccionadas, evitando duplicados en la lista de guardado.
     */
    private void guardarImagenesSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> selectedUris = adapter.getSelectedItems();
        int countAdded = 0;

        // VALIDACIÓN DE DUPLICADOS: Añadir solo los ítems nuevos a la lista MAESTRA.
        Set<Uri> currentSavedSet = new HashSet<>(listaSeleccionadaParaGuardar);

        for (Uri uri : selectedUris) {
            if (!currentSavedSet.contains(uri)) {
                listaSeleccionadaParaGuardar.add(uri);
                currentSavedSet.add(uri);
                countAdded++;
            }
        }

        // CORRECCIÓN CLAVE: Almacenar la lista final consolidada en el ViewModel
        // para que otros botones puedan usarla para navegar.
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);

        // Notificar al fragmento padre sobre la selección
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

        // CORRECCIÓN CLAVE: Botones de Edición, Recorte y Eliminación
        // usan la lista MAESTRA (listaSeleccionadaParaGuardar) del ViewModel

        if (id == R.id.recortar2) {
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para recortar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
        } else if (id == R.id.edicion2) {
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para editar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);
        } else if (id == R.id.eliminar2) {
            // CORRECCIÓN: Navegar a eliminación con la lista MAESTRA, no la selección de la galería
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(getContext(), "No hay elementos guardados para eliminar.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Almacenar la lista MAESTRA en el ViewModel (ya está actualizado en guardarImagenesSeleccionadas,
            // pero lo hacemos aquí para asegurarnos antes de navegar)
            sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);

            // 2. Navegar al fragmento de eliminación
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);

            // 3. La selección visual del RecyclerView se limpia, ya que no se usó
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
        }
    }
}