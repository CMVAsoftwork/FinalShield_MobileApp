package com.example.finalshield.Fragments.Escaner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.example.finalshield.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EliminarPaginas extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {
    ImageButton camara,galeria, edicion, recortar;
    private RecyclerView recycler;
    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button clearSelection;
    private Button eliminarSeleccion; // Este activa la eliminación
    private LinearLayout dialogContainer;
    private Button siEliminarBtn;
    private Button noEliminarBtn;
    private Button regresar;

    // Data Management
    private SharedImageViewModel sharedViewModel;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>();

    // Claves y IDs
    public static final String KEY_ACTUALIZACION_LISTA = "eliminacion_lista_actualizada";
    public static final String BUNDLE_RESULT_URI_LIST = "resultado_uri_list";
    private static final int DESTINO_CAMARA_ID = R.id.escanerCifradoMixto;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_eliminar_paginas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // 1. Inicialización de UI (Se mantiene igual)
        recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar3);
        galeria = v.findViewById(R.id.selecgaleria);
        edicion = v.findViewById(R.id.edicion);
        recortar = v.findViewById(R.id.recortar);
        camara = v.findViewById(R.id.scancam);

        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        clearSelection = v.findViewById(R.id.clearSelection);
        eliminarSeleccion = v.findViewById(R.id.eliminarSeleccion);

        dialogContainer = v.findViewById(R.id.dialogContainer);
        siEliminarBtn = v.findViewById(R.id.sieliminar);
        noEliminarBtn = v.findViewById(R.id.noeliminar);

        // 2. Listeners de la UI (Se mantiene igual, solo se actualiza 'siEliminarBtn')
        regresar.setOnClickListener(this);
        galeria.setOnClickListener(this);
        edicion.setOnClickListener(this);
        recortar.setOnClickListener(this);
        camara.setOnClickListener(this);

        clearSelection.setOnClickListener(view -> {
            if (adapter != null) {
                adapter.clearSelection();
                selectionBar.setVisibility(View.GONE);
            }
        });

        eliminarSeleccion.setOnClickListener(view -> {
            if (adapter != null && adapter.getSelectedCount() > 0) mostrarDialogoConfirmacion();
            else Toast.makeText(getContext(), "Selecciona elementos para eliminar.", Toast.LENGTH_SHORT).show();
        });

        siEliminarBtn.setOnClickListener(view -> {
            eliminarFotosSeleccionadas();
            ocultarDialogoConfirmacion();
        });

        noEliminarBtn.setOnClickListener(view -> ocultarDialogoConfirmacion());

        // 3. Cargar la lista del ViewModel
        cargarFotosDesdeViewModel();
    }

    // --- MANEJO DE DATOS ---

    private void cargarFotosDesdeViewModel() {
        List<Uri> selectedUris = sharedViewModel.getImageUriList();

        if (selectedUris.isEmpty()) {
            Toast.makeText(getContext(), "No hay fotos para eliminar.", Toast.LENGTH_LONG).show();
            // 🌟 Usamos popBackStack para regresar al fragmento anterior (probablemente EscanerCifradoMixto o Galería)
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        listaImagenesGaleria.clear();
        listaImagenesGaleria.addAll(selectedUris);

        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.item_imagen, true);
        recycler.setAdapter(adapter);
    }

    private void mostrarDialogoConfirmacion() {
        if (dialogContainer != null) dialogContainer.setVisibility(View.VISIBLE);
    }

    private void ocultarDialogoConfirmacion() {
        if (dialogContainer != null) dialogContainer.setVisibility(View.GONE);
    }

    private void eliminarFotosSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        // 1. Ejecutar la eliminación en el adaptador (que modifica listaImagenesGaleria)
        List<Uri> deletedUris = adapter.discardSelectedItems();
        int count = deletedUris.size();

        // 2. 🌟 ACTUALIZAR el ViewModel con la lista combinada modificada
        sharedViewModel.setImageUriList(listaImagenesGaleria);

        // 3. 🌟 CLAVE: Sincronizar la lista de solo cámara para eliminar las URIs borradas.
        actualizarListaSoloCamara(deletedUris);

        // 4. Opcional: Eliminar archivos de caché
        eliminarArchivosFisicos(deletedUris);

        selectionBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), count + " fotos eliminadas.", Toast.LENGTH_SHORT).show();

        if (listaImagenesGaleria.isEmpty()) {
            Toast.makeText(getContext(), "Todas las fotos han sido eliminadas. Regresando.", Toast.LENGTH_SHORT).show();
            // 5. Navegar automáticamente si la lista queda vacía
            finalizarYRegresarACamara();
        }
    }

    /**
     * Filtra y actualiza la lista de fotos de solo cámara en el ViewModel.
     */
    private void actualizarListaSoloCamara(List<Uri> deletedUris) {
        List<Uri> cameraList = sharedViewModel.getCameraOnlyUriList();
        if (cameraList.isEmpty()) return;

        // Creamos una nueva lista de cámara sin los elementos eliminados
        List<Uri> nuevaListaCamara = new ArrayList<>(cameraList);
        nuevaListaCamara.removeAll(deletedUris);

        // Actualizamos el ViewModel
        sharedViewModel.setCameraOnlyUriList(nuevaListaCamara);
    }


    /**
     * Intenta eliminar los archivos físicos asociados a los URIs eliminados.
     */
    private void eliminarArchivosFisicos(List<Uri> uris) {
        for (Uri uri : uris) {
            try {
                // Asumiendo que solo los URIs de FileProvider (content scheme) apuntan a archivos en caché
                if ("content".equals(uri.getScheme())) {
                    String fileName = uri.getLastPathSegment();
                    if (fileName != null) {
                        File cacheDir = requireContext().getCacheDir();
                        // Nota: Los archivos de Galería copiados empiezan con 'editable_'
                        // Los archivos de Cámara empiezan con 'CAMARA_TEMP_'
                        File fileToDelete = new File(cacheDir, fileName);
                        if (fileToDelete.exists()) {
                            boolean deleted = fileToDelete.delete();
                            if (!deleted) Log.w("EliminarPaginas", "Fallo al eliminar archivo: " + fileName);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("EliminarPaginas", "Error al intentar eliminar archivo físico: " + uri.toString(), e);
            }
        }
    }


    /**
     * Finaliza la edición y navega de regreso al destino principal.
     */
    private void finalizarYRegresarADestino() {
        // En este punto, sharedViewModel.getImageUriList() ya tiene la lista final.
        // Usaremos popBackStack() para volver al fragmento que estaba anteriormente (EscanerCifradoMixto).
        Navigation.findNavController(requireView()).popBackStack();
    }
    private void finalizarYRegresarACamara() {
        // En este punto, sharedViewModel.getImageUriList() ya tiene la lista final.
        // Usaremos popBackStack() para volver al fragmento que estaba anteriormente (EscanerCifradoMixto).
        Navigation.findNavController(requireView()).navigate(DESTINO_CAMARA_ID);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.guardar || v.getId() == R.id.regresar3) {
            finalizarYRegresarADestino();
        } else if(v.getId() == R.id.scancam){
            // Usamos navigate, no popBackStack, para ir a un fragmento específico.
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        } else if (v.getId() == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(R.id.seleccion_imagenes);
        } else if (v.getId() == R.id.edicion) {
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (v.getId() == R.id.recortar) {
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        }
    }

    // ... (onImageClicked y onSelectionChanged se mantienen igual)

    @Override
    public void onImageClicked(Uri uri) {
        if (adapter != null && !adapter.isSelectionMode()) {
            ArrayList<String> uriStringList = new ArrayList<>();
            for (Uri u : listaImagenesGaleria) uriStringList.add(u.toString());

            int position = listaImagenesGaleria.indexOf(uri);
            if (position == -1) return;

            // Asegúrate de que VistaImagenActivity exista
            Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
            intent.putStringArrayListExtra("uri_list", uriStringList);
            intent.putExtra("position", position);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count > 0) {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText(count + " seleccionadas");
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }
}