package com.example.finalshield.Fragments.EscanerGa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.SimpleItemTouchHelperCallback;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class EscanerGaVisualizacionYReordenamiento extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    private ImageButton selecimg2, eliminar2, recortar2, edicion2;
    private Button regresar;
    private RecyclerView recycler;

    private SharedImageViewModel sharedViewModel;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>();

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
        return inflater.inflate(R.layout.fragment_escaner_ga_visualizacion_y_reordenamiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recycler = v.findViewById(R.id.recyclerR);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar3);
        selecimg2 = v.findViewById(R.id.selecgaleria2);
        eliminar2 = v.findViewById(R.id.eliminar2);
        recortar2 = v.findViewById(R.id.recortar2);
        edicion2 = v.findViewById(R.id.edicion2);

        selecimg2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        recortar2.setOnClickListener(this);
        regresar.setOnClickListener(this);
        edicion2.setOnClickListener(this);

        cargarFotosDesdeViewModel();
    }

    private void cargarFotosDesdeViewModel() {
        List<Uri> selectedUris = sharedViewModel.getImageUriList();

        // CORRECCIÓN: Si el ViewModel está vacío pero la lista local tiene algo, no sacamos al usuario.
        // Solo regresamos si de verdad no hay datos en ninguna parte.
        if ((selectedUris == null || selectedUris.isEmpty()) && listaImagenesGaleria.isEmpty()) {
            Navigation.findNavController(requireView()).popBackStack(DESTINO_GALERIA_ID, false);
            return;
        }

        if (selectedUris != null && !selectedUris.isEmpty()) {
            listaImagenesGaleria.clear();
            listaImagenesGaleria.addAll(selectedUris);
        }

        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.item_imagen, false);
        recycler.setAdapter(adapter);

        try {
            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recycler);
        } catch (Exception e) {
            Log.e("Reordenar", "Error ItemTouchHelper", e);
        }
    }

    private void regresarYGuardarAGaleriaConResultado() {
        // Obtenemos el orden actual del adapter
        List<Uri> listaOrdenada = (adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria;

        // 1. Sincronizar ViewModel de inmediato
        sharedViewModel.setImageUriList(new ArrayList<>(listaOrdenada));

        // 2. Notificar a la Galería mediante FragmentResult
        Bundle result = new Bundle();
        ArrayList<String> urisStr = new ArrayList<>();
        for (Uri uri : listaOrdenada) urisStr.add(uri.toString());

        result.putStringArrayList(BUNDLE_RESULT_URI_LIST, urisStr);
        getParentFragmentManager().setFragmentResult(KEY_ACTUALIZACION_LISTA, result);

        Navigation.findNavController(requireView()).popBackStack(DESTINO_GALERIA_ID, false);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Antes de cualquier navegación, capturamos el estado actual para no perder cambios
        List<Uri> currentOrder = (adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria;
        sharedViewModel.setImageUriList(new ArrayList<>(currentOrder));

        if (id == R.id.regresar3 || id == R.id.selecgaleria2) {
            regresarYGuardarAGaleriaConResultado();
        } else if (id == R.id.eliminar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);
        } else if (id == R.id.recortar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
        } else if (id == R.id.edicion2) {
            Toast.makeText(getContext(), "Ya estás aquí.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageClicked(Uri uri) {
        List<Uri> currentOrder = (adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria;
        ArrayList<String> uriStringList = new ArrayList<>();
        for (Uri u : currentOrder) uriStringList.add(u.toString());

        int position = currentOrder.indexOf(uri);
        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        intent.putStringArrayListExtra("uri_list", uriStringList);
        intent.putExtra("position", position);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) {}
}