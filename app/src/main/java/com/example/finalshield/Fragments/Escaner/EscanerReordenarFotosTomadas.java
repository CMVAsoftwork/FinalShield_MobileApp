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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.SimpleItemTouchHelperCallback;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class EscanerReordenarFotosTomadas extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks{
    private RecyclerView recycler;
    private ImageAdapter adapter;
    private SharedImageViewModel sharedViewModel;
    private List<Uri> listaFotosCamara = new ArrayList<>();
    private View regresar;
    private View guardar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Asegúrate de que este layout exista (manteniendo el layout original si contiene el recycler)
        return inflater.inflate(R.layout.fragment_escaner_reordenar_fotos_tomadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recycler = v.findViewById(R.id.recycler);
        // Usamos GridLayoutManager para la vista de cuadrícula
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar1);
        guardar = v.findViewById(R.id.guardar);

        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        cargarFotosDesdeViewModel();

        // ❌ ELIMINADO: Se elimina la inicialización y el attachment de ItemTouchHelper
        // ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        // ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        // touchHelper.attachToRecyclerView(recycler);
    }

    private void cargarFotosDesdeViewModel() {
        listaFotosCamara.clear();

        // Usamos el método dedicado para la lista SOLO DE CÁMARA.
        List<Uri> cameraOnlyList = sharedViewModel.getCameraOnlyUriList();

        if (!cameraOnlyList.isEmpty()) {
            listaFotosCamara.addAll(cameraOnlyList);
        } else {
            // Ajustamos el mensaje ya que solo es visualización
            Toast.makeText(getContext(), "No hay fotos tomadas para visualizar.", Toast.LENGTH_SHORT).show();
        }

        // Asegúrate de que ImageAdapter esté correctamente implementado
        adapter = new ImageAdapter(listaFotosCamara, this, R.layout.item_imagen);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.regresar1 || id == R.id.guardar) {
            guardarYSalir();
        }
    }

    private void guardarYSalir() {
        // Obtenemos la lista actual. Dado que no hay reordenamiento, es la misma que la cargada.
        // Mantenemos la llamada a setCameraOnlyUriList para confirmar el estado actual.
        List<Uri> finalUris = listaFotosCamara; // No necesitamos getRetainedItems() ya que no se modificó el orden.

        sharedViewModel.setCameraOnlyUriList(finalUris);

        Toast.makeText(getContext(), "Visualización completada. Regresando.", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }

    // El método onImageClicked permanece para permitir la vista ampliada
    @Override
    public void onImageClicked(Uri uri) {
        // Usamos la lista de la clase ya que no hubo reordenamiento
        List<Uri> currentUris = listaFotosCamara;

        ArrayList<String> uriStringList = new ArrayList<>();
        for (Uri u : currentUris) uriStringList.add(u.toString());

        int position = currentUris.indexOf(uri);
        if (position == -1) return;

        // Asegúrate de que VistaImagenActivity exista
        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        intent.putStringArrayListExtra("uri_list", uriStringList);
        intent.putExtra("position", position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) { /* No usado en visualización */ }
}