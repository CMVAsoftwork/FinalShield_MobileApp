package com.example.finalshield.Fragments.EscanerGa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class EscanerGaEliminarPaginas extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    ImageButton selecimg2, edicion2, recortar2;
    private RecyclerView recycler;
    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button clearSelection, eliminarSeleccion, regresar;
    private LinearLayout dialogContainer;
    private Button siEliminarBtn, noEliminarBtn;

    private SharedImageViewModel sharedViewModel;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>();

    // Debe coincidir con el listener de EscanerCifradoGaleria
    public static final String KEY_ACTUALIZACION_LISTA = "actualizacion_lista";
    public static final String BUNDLE_RESULT_URI_LIST = "uri_list";
    private static final int DESTINO_GALERIA_ID = R.id.escanerCifradoGaleria2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ga_eliminar_paginas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

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
            if (adapter != null && adapter.getSelectedCount() > 0) dialogContainer.setVisibility(View.VISIBLE);
            else Toast.makeText(getContext(), "Selecciona elementos para eliminar.", Toast.LENGTH_SHORT).show();
        });

        siEliminarBtn.setOnClickListener(view -> {
            eliminarFotosSeleccionadas();
            dialogContainer.setVisibility(View.GONE);
        });

        noEliminarBtn.setOnClickListener(view -> dialogContainer.setVisibility(View.GONE));

        cargarFotosDesdeViewModel();
    }

    private void cargarFotosDesdeViewModel() {
        List<Uri> selectedUris = sharedViewModel.getImageUriList();
        if (selectedUris.isEmpty()) {
            Navigation.findNavController(requireView()).popBackStack(DESTINO_GALERIA_ID, false);
            return;
        }

        listaImagenesGaleria.clear();
        listaImagenesGaleria.addAll(selectedUris);

        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.item_imagen, true);
        recycler.setAdapter(adapter);
    }

    private void eliminarFotosSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        // 1. Obtener qué se va a borrar
        List<Uri> aEliminar = adapter.getSelectedItems();

        // 2. Modificar la lista local
        listaImagenesGaleria.removeAll(aEliminar);

        // 3. Notificar al adaptador y limpiar selección
        adapter.discardSelectedItems(); // Asumiendo que esto limpia internamente la selección del adapter

        // 4. Sincronizar con el ViewModel inmediatamente
        sharedViewModel.setImageUriList(new ArrayList<>(listaImagenesGaleria));

        Toast.makeText(getContext(), aEliminar.size() + " eliminadas.", Toast.LENGTH_SHORT).show();

        if (listaImagenesGaleria.isEmpty()) {
            finalizarYRegresarAGaleriaConResultado();
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }

    private void finalizarYRegresarAGaleriaConResultado() {
        // Actualizar ViewModel una última vez por seguridad
        sharedViewModel.setImageUriList(new ArrayList<>(listaImagenesGaleria));

        // Empaquetar para el FragmentResult (esto refresca la galería si está escuchando)
        Bundle result = new Bundle();
        ArrayList<String> retainedUrisStr = new ArrayList<>();
        for (Uri uri : listaImagenesGaleria) retainedUrisStr.add(uri.toString());

        result.putStringArrayList(BUNDLE_RESULT_URI_LIST, retainedUrisStr);
        getParentFragmentManager().setFragmentResult(KEY_ACTUALIZACION_LISTA, result);

        Navigation.findNavController(requireView()).popBackStack(DESTINO_GALERIA_ID, false);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Antes de moverte a cualquier lado, guarda el estado actual
        sharedViewModel.setImageUriList(new ArrayList<>(listaImagenesGaleria));

        if (id == R.id.selecgaleria2 || id == R.id.regresar3) {
            finalizarYRegresarAGaleriaConResultado();
        } else if (id == R.id.edicion2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);
        } else if (id == R.id.recortar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
        }
    }

    @Override
    public void onImageClicked(Uri uri) {
        if (adapter != null && !adapter.isSelectionMode()) {
            ArrayList<String> uriStringList = new ArrayList<>();
            for (Uri u : listaImagenesGaleria) uriStringList.add(u.toString());
            int position = listaImagenesGaleria.indexOf(uri);

            Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
            intent.putStringArrayListExtra("uri_list", uriStringList);
            intent.putExtra("position", position);
            startActivity(intent);
        }
    }

    @Override
    public void onSelectionChanged(int count) {
        selectionBar.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) selectionCount.setText(count + " seleccionadas");
    }
}