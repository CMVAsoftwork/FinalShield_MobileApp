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


public class EscanerGaVisualizacionYReordenamiento extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    // UI Elements
    ImageButton selecimg2, eliminar2, recortar2, edicion2;
    private Button regresar, guardarHecho;
    private RecyclerView recycler;

    // Data Management
    private SharedImageViewModel sharedViewModel;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>();

    // Claves para el FragmentResult
    public static final String KEY_ACTUALIZACION_LISTA = "reordenamiento_lista_actualizada";
    public static final String BUNDLE_RESULT_URI_LIST = "resultado_uri_list";

    // ID del fragmento de la Galería
    private static final int DESTINO_GALERIA_ID = R.id.escanerCifradoGaleria2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ga_visualizacion_y_reordenamiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // 1. Inicialización de UI
        recycler = v.findViewById(R.id.recyclerR);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar3);
        guardarHecho = v.findViewById(R.id.guardar);
        selecimg2 = v.findViewById(R.id.selecgaleria2);
        eliminar2 = v.findViewById(R.id.eliminar2);
        recortar2 = v.findViewById(R.id.recortar2);
        edicion2 = v.findViewById(R.id.edicion2);

        // 2. Listeners
        selecimg2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        recortar2.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardarHecho.setOnClickListener(this);
        edicion2.setOnClickListener(this);

        // 3. Cargar datos y configurar RecyclerView
        cargarFotosDesdeViewModel();
    }

    // --- LÓGICA DE DATOS Y FLUJO ---

    private void cargarFotosDesdeViewModel() {
        listaImagenesGaleria.clear();

        List<Uri> selectedUris = sharedViewModel.getImageUriList();

        if (selectedUris.isEmpty()) {
            Toast.makeText(getContext(), "No hay fotos para visualizar/reordenar. Regresando a Galería.", Toast.LENGTH_LONG).show();
            // Si la lista está vacía, saltamos a Galería
            regresarYGuardarAGaleriaConResultado();
            return;
        }

        listaImagenesGaleria.addAll(selectedUris);

        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.item_imagen, false);
        recycler.setAdapter(adapter);

        try {
            // Asegúrate de que SimpleItemTouchHelperCallback y ImageAdapter estén en el proyecto
            ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recycler);
        } catch (NoClassDefFoundError | Exception e) {
            Log.e("Reordenar", "Error al configurar ItemTouchHelper: Asegúrate de que SimpleItemTouchHelperCallback exista.", e);
        }
    }

    /**
     * Empaqueta la lista reordenada, actualiza el ViewModel y navega directamente a Galería.
     */
    private void regresarYGuardarAGaleriaConResultado() {
        Bundle result = new Bundle();
        ArrayList<String> urisStr = new ArrayList<>();

        List<Uri> finalUris = (adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria;

        for (Uri uri : finalUris) urisStr.add(uri.toString());

        // 1. Actualizar el ViewModel
        sharedViewModel.setImageUriList(finalUris);

        // 2. Enviar el resultado al FragmentManager
        result.putStringArrayList(BUNDLE_RESULT_URI_LIST, urisStr);
        getParentFragmentManager().setFragmentResult(KEY_ACTUALIZACION_LISTA, result);

        Toast.makeText(getContext(), "Lista actualizada y reordenada.", Toast.LENGTH_SHORT).show();

        // 3. Navega DIRECTAMENTE al fragmento de la Galería (EscanerCifradoGaleria2)
        Navigation.findNavController(requireView()).popBackStack(DESTINO_GALERIA_ID, false);
    }

    // --- MANEJO DE CLICKS Y NAVEGACIÓN ---

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // ✅ CORRECCIÓN CLAVE: Botones de FINALIZACIÓN regresan directamente a Galería
        if (id == R.id.regresar3 || id == R.id.guardar || id == R.id.selecgaleria2) {
            regresarYGuardarAGaleriaConResultado();

            // 2. NAVEGACIÓN A OTRAS HERRAMIENTAS
        } else if (id == R.id.eliminar2) {
            if (listaImagenesGaleria.isEmpty()) {
                Toast.makeText(getContext(), "Lista vacía. Regresando a Galería.", Toast.LENGTH_SHORT).show();
                regresarYGuardarAGaleriaConResultado();
                return;
            }
            // Sincroniza el ViewModel con la lista reordenada y navega
            sharedViewModel.setImageUriList((adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria);
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);

        } else if (id == R.id.recortar2) {
            if (listaImagenesGaleria.isEmpty()) {
                Toast.makeText(getContext(), "Lista vacía. Regresando a Galería.", Toast.LENGTH_SHORT).show();
                regresarYGuardarAGaleriaConResultado();
                return;
            }
            // Sincroniza el ViewModel con la lista reordenada y navega
            sharedViewModel.setImageUriList((adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria);
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);

        } else if (id == R.id.edicion2) {
            Toast.makeText(getContext(), "Ya estás en Visualización y Reordenamiento.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onImageClicked(Uri uri) {
        // Implementación para ver la imagen en pantalla completa
        ArrayList<String> uriStringList = new ArrayList<>();
        List<Uri> currentUris = (adapter != null) ? adapter.getRetainedItems() : listaImagenesGaleria;

        for (Uri u : currentUris) uriStringList.add(u.toString());

        int position = currentUris.indexOf(uri);
        if (position == -1) return;

        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        intent.putStringArrayListExtra("uri_list", uriStringList);
        intent.putExtra("position", position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) { /* No usado */ }
}