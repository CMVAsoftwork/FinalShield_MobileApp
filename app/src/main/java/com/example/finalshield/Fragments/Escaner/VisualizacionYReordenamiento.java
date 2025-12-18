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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.SimpleItemTouchHelperCallback;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class VisualizacionYReordenamiento extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks{

    // IDs de destino
    private static final int DESTINO_CAMARA_ID = R.id.escanerCifradoMixto;
    private static final int DESTINO_GALERIA_ID = R.id.seleccion_imagenes;
    private static final int DESTINO_RECORTAR_ID = R.id.cortarRotar;
    private static final int DESTINO_ELIMINAR_ID = R.id.eliminarPaginas;

    ImageButton galeria, camara, recortar, eliminar;
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
        return inflater.inflate(R.layout.fragment_visualizacion_y_reordenamiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recycler = v.findViewById(R.id.recyclerR);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar3);
        guardar = v.findViewById(R.id.guardar);

        camara = v.findViewById(R.id.scancam);
        galeria = v.findViewById(R.id.selecgaleria);
        recortar = v.findViewById(R.id.recortar);
        eliminar = v.findViewById(R.id.eliminar);

        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        if (camara != null) camara.setOnClickListener(this);
        if (galeria != null) galeria.setOnClickListener(this);
        if (recortar != null) recortar.setOnClickListener(this);
        if (eliminar != null) eliminar.setOnClickListener(this);

        cargarFotosDesdeViewModel();

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);
    }

    private void cargarFotosDesdeViewModel() {
        listaFotosCamara.clear();

        // 🌟 Carga la lista COMBINADA
        List<Uri> viewModelList = sharedViewModel.getImageUriList();
        if (!viewModelList.isEmpty()) {
            listaFotosCamara.addAll(viewModelList);
        } else {
            Toast.makeText(getContext(), "No hay imágenes cargadas para visualizar.", Toast.LENGTH_SHORT).show();
        }

        adapter = new ImageAdapter(listaFotosCamara, this, R.layout.item_imagen, false);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // 🌟 Sincronizar el orden actual de la lista COMBINADA antes de navegar
        if (adapter != null) {
            listaFotosCamara = adapter.getRetainedItems();
            sharedViewModel.setImageUriList(listaFotosCamara);
        }

        if (id == R.id.regresar1 || id == R.id.guardar) {
            guardarYSalir();
        } else if(id == R.id.scancam){
            Navigation.findNavController(v).navigate(DESTINO_CAMARA_ID);
        } else if (id == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(DESTINO_GALERIA_ID);
        } else if (id == R.id.recortar) {
            Navigation.findNavController(v).navigate(DESTINO_RECORTAR_ID);
        } else if (id == R.id.eliminar) {
            Navigation.findNavController(v).navigate(DESTINO_ELIMINAR_ID);
        }
    }

    private void guardarYSalir() {
        List<Uri> finalUris = (adapter != null) ? adapter.getRetainedItems() : listaFotosCamara;
        sharedViewModel.setImageUriList(finalUris);
        Toast.makeText(getContext(), "Imágenes reordenadas y guardadas.", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onImageClicked(Uri uri) {
        if (adapter != null) {
            listaFotosCamara = adapter.getRetainedItems();
        }

        ArrayList<String> uriStringList = new ArrayList<>();
        for (Uri u : listaFotosCamara) uriStringList.add(u.toString());

        int position = listaFotosCamara.indexOf(uri);
        if (position == -1) return;

        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        intent.putStringArrayListExtra("uri_list", uriStringList);
        intent.putExtra("position", position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) { /* No usado en reordenar */ }
}