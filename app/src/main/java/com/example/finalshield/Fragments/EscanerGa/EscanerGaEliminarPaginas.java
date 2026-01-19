package com.example.finalshield.Fragments.EscanerGa;

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

import java.util.ArrayList;
import java.util.List;

public class EscanerGaEliminarPaginas extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    // UI Elements
    ImageButton selecimg2, edicion2, recortar2;
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
    private static final int DESTINO_GALERIA_ID = R.id.escanerCifradoGaleria2; // ID del fragmento de la Galería


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ga_eliminar_paginas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // 1. Inicialización de UI
        recycler = v.findViewById(R.id.recycler_eliminacion);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar3);
        selecimg2 = v.findViewById(R.id.selecgaleria2);
        edicion2 = v.findViewById(R.id.edicion2);
        recortar2 = v.findViewById(R.id.recortar2);

        selectionBar = v.findViewById(R.id.selectionBarDelete);
        selectionCount = v.findViewById(R.id.selectionCount);
        clearSelection = v.findViewById(R.id.clearSelection);
        eliminarSeleccion = v.findViewById(R.id.eliminarSeleccion);

        dialogContainer = v.findViewById(R.id.dialogContainer);
        siEliminarBtn = v.findViewById(R.id.sieliminar);
        noEliminarBtn = v.findViewById(R.id.noeliminar);

        // 2. Listeners de la UI
        regresar.setOnClickListener(this);
        selecimg2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        recortar2.setOnClickListener(this);

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
            Toast.makeText(getContext(), "No hay fotos para eliminar. Regresando a Galería.", Toast.LENGTH_LONG).show();
            // Si la lista está vacía al inicio, finalizamos y volvemos a Galería
            finalizarYRegresarAGaleriaConResultado();
            return;
        }

        listaImagenesGaleria.clear();
        listaImagenesGaleria.addAll(selectedUris);

        // Asumo que tu ImageAdapter acepta una lista de Uri, un Callback y un layout ID
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

        // Asume que discardSelectedItems() limpia la selección y retorna la lista de elementos eliminados
        List<Uri> deletedUris = adapter.discardSelectedItems();
        int count = deletedUris.size();

        selectionBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), count + " fotos eliminadas de la vista.", Toast.LENGTH_SHORT).show();

        if (listaImagenesGaleria.isEmpty()) {
            Toast.makeText(getContext(), "Todas las fotos han sido eliminadas. Regresando a Galería.", Toast.LENGTH_SHORT).show();
            // Si la lista queda vacía, finalizamos y volvemos a Galería
            finalizarYRegresarAGaleriaConResultado();
        }
    }

    /**
     * Empaqueta la lista actual de URIs (modificada o no), actualiza el ViewModel y navega directamente a Galería.
     */
    private void finalizarYRegresarAGaleriaConResultado() {
        // 1. Siempre se actualiza el ViewModel antes de salir
        sharedViewModel.setImageUriList(listaImagenesGaleria);

        Bundle result = new Bundle();
        ArrayList<String> retainedUrisStr = new ArrayList<>();

        for (Uri uri : listaImagenesGaleria) {
            retainedUrisStr.add(uri.toString());
        }

        // 2. Enviar el resultado al FragmentManager
        result.putStringArrayList(BUNDLE_RESULT_URI_LIST, retainedUrisStr);
        getParentFragmentManager().setFragmentResult(KEY_ACTUALIZACION_LISTA, result);

        // 3. Navega DIRECTAMENTE al fragmento de la Galería (EscanerCifradoGaleria2)
        Navigation.findNavController(requireView()).popBackStack(DESTINO_GALERIA_ID, false);
    }

    // --- MANEJO DE NAVEGACIÓN ---

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // ✅ FINALIZACIÓN: Botones que guardan la lista y regresan directamente a Galería
        if (id == R.id.selecgaleria2 || id == R.id.regresar3 || id == R.id.guardar) {
            finalizarYRegresarAGaleriaConResultado();

            // NAVEGACIÓN A OTRAS HERRAMIENTAS: Sincroniza ViewModel y navega
        } else if (id == R.id.edicion2) {
            if (listaImagenesGaleria.isEmpty()) {
                Toast.makeText(getContext(), "No hay fotos para editar.", Toast.LENGTH_SHORT).show();
                finalizarYRegresarAGaleriaConResultado();
                return;
            }
            // Sincroniza el ViewModel con el estado actual
            sharedViewModel.setImageUriList(listaImagenesGaleria);
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);

        } else if (id == R.id.recortar2) {
            if (listaImagenesGaleria.isEmpty()) {
                Toast.makeText(getContext(), "No hay fotos para recortar.", Toast.LENGTH_SHORT).show();
                finalizarYRegresarAGaleriaConResultado();
                return;
            }
            // Sincroniza el ViewModel con el estado actual
            sharedViewModel.setImageUriList(listaImagenesGaleria);
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
        }
    }


    @Override
    public void onImageClicked(Uri uri) {
        // Implementa aquí la lógica para ver la imagen a pantalla completa (VistaImagenActivity)
        if (adapter != null && !adapter.isSelectionMode()) {
            ArrayList<String> uriStringList = new ArrayList<>();
            for (Uri u : listaImagenesGaleria) uriStringList.add(u.toString());

            int position = listaImagenesGaleria.indexOf(uri);
            if (position == -1) return;

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