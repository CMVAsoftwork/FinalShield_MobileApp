package com.example.finalshield.Fragments.EscanerCa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SimpleItemTouchHelperCallback;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EscanerCaReordenar extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {
    private RecyclerView recycler;
    private ImageAdapter adapter;
    private final List<Uri> listaFotosCamara = new ArrayList<>();
    private Button regresar;
    private Button guardar;

    // Claves para el FragmentResultListener (las definimos aquí para mantenerlas centralizadas)
    public static final String KEY_REORDENAR_RESULT = "reordenar_key";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_reordenar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar1);
        guardar = v.findViewById(R.id.guardar);

        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        cargarFotosDesdeArgumentos();

        // **CONFIGURACIÓN DEL DRAG AND DROP CON ITEMTOUCHHELPER**
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);

        // **SOLUCIÓN AL PROBLEMA DE SELECCIÓN:** Deshabilitar Long Click
        deshabilitarLongClickEnItems();
    }

    /**
     * CLAVE: Desactiva el OnLongClickListener de cada item del RecyclerView.
     * Esto evita que el ImageAdapter inicie el selectionMode al mantener presionado,
     * permitiendo que ItemTouchHelper tome control para el arrastre.
     */
    private void deshabilitarLongClickEnItems() {
        recycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                // Anula el OnLongClickListener que inicia el modo de selección
                view.setOnLongClickListener(null);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                // No es necesario hacer nada aquí
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.regresar1 || id == R.id.guardar) {
            // Recolectar la lista de URIs EN EL ORDEN ACTUALIZADO
            Bundle result = new Bundle();
            ArrayList<String> reordenadasStr = new ArrayList<>();

            // Recorremos la lista que ya fue reordenada en memoria por el adaptador
            for (Uri uri : listaFotosCamara) {
                // **CLAVE:** Enviamos el String de la URI, pero en EscanerCifradoCamara
                // tendremos que manejar la resolución a File nuevamente.
                reordenadasStr.add(uri.toString());
            }

            result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, reordenadasStr);

            // Enviamos el resultado al Fragmento anterior
            getParentFragmentManager().setFragmentResult(KEY_REORDENAR_RESULT, result);

            // Regresamos
            Navigation.findNavController(v).popBackStack();
        }
    }

    private void cargarFotosDesdeArgumentos() {
        listaFotosCamara.clear();
        Bundle args = getArguments();

        if (args != null) {
            // RECIBIMOS RUTAS ABSOLUTAS (String) del Fragmento anterior
            ArrayList<String> filePaths = args.getStringArrayList("FOTOS_CAPTURA");

            if (filePaths != null) {
                for (String path : filePaths) {
                    File file = new File(path);
                    if (file.exists()) {
                        // crear URI con FileProvider
                        Uri fileUri = FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName() + ".fileprovider",
                                file);
                        // Guardamos el URI en la lista
                        listaFotosCamara.add(fileUri);
                    }
                }
            }
        }
        adapter = new ImageAdapter(listaFotosCamara, this);

        recycler.setAdapter(adapter);
    }

    // Implementación de la interfaz ImageAdapter.Callbacks

    @Override
    public void onImageClicked(Uri uri) {
        // La lista de URIs ya existe como 'listaFotosCamara' en EscanerCaReordenar

        // 1. Convertir la lista de Uri a lista de String
        ArrayList<String> uriStringList = new ArrayList<>();
        for (Uri u : listaFotosCamara) {
            uriStringList.add(u.toString());
        }

        // 2. Encontrar la posición del URI clickeado
        int position = listaFotosCamara.indexOf(uri);

        // 3. Iniciar la actividad con la lista y la posición
        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);

        // Usar las CLAVES estáticas de la nueva VistaImagenActivity
        intent.putStringArrayListExtra(VistaImagenActivity.EXTRA_URI_LIST, uriStringList);
        intent.putExtra(VistaImagenActivity.EXTRA_POSITION, position);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) {
        // Este método se mantiene vacío porque solo usamos el adaptador para arrastrar/clic
        // y no para el modo de selección. Si en otras vistas lo usas, se mantiene.
    }
}