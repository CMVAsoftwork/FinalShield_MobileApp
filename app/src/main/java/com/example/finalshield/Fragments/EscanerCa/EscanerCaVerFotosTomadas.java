package com.example.finalshield.Fragments.EscanerCa;

import static com.example.finalshield.Fragments.EscanerCa.EscanerCaReordenar.BUNDLE_REORDENAR_URI_LIST;
import static com.example.finalshield.Fragments.EscanerCa.EscanerCaReordenar.KEY_REORDENAR_RESULT;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
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
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EscanerCaVerFotosTomadas extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {

    // Claves de resultado para EscanerCifradoCamara
    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";

    // Vistas y Componentes
    ImageButton cam1, cortar1;
    private RecyclerView recycler;
    private ImageAdapter adapter;
    private final List<Uri> listaFotosCamara = new ArrayList<>();

    // Barras de UI
    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button clearSelection;
    private Button descartarSeleccion;
    private Button regresar;
    private Button guardar;

    // Elementos del Diálogo de Confirmación
    private LinearLayout dialogContainer;
    private Button siEliminarBtn;
    private Button noEliminarBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_ver_fotos_tomadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // 1. Inicialización de Vistas
        recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar1);
        cam1 = v.findViewById(R.id.scancam1);
        cortar1 = v.findViewById(R.id.recortar1);
        guardar = v.findViewById(R.id.guardar);

        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        clearSelection = v.findViewById(R.id.clearSelection);
        descartarSeleccion = v.findViewById(R.id.descartarSeleccion);

        // Inicialización de las vistas del Diálogo (IDs del XML modificado)
        dialogContainer = v.findViewById(R.id.dialogContainer);
        siEliminarBtn = v.findViewById(R.id.sieliminar);
        noEliminarBtn = v.findViewById(R.id.noeliminar);

        // 2. Listeners
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);
        cam1.setOnClickListener(this);
        cortar1.setOnClickListener(this);

        clearSelection.setOnClickListener(view -> {
            if (adapter != null) {
                adapter.clearSelection();
                selectionBar.setVisibility(View.GONE);
            }
        });

        // Lógica: Muestra el diálogo de confirmación en lugar de eliminar directamente
        descartarSeleccion.setOnClickListener(view -> {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                mostrarDialogoConfirmacion();
            }
        });

        // Listeners del Diálogo
        siEliminarBtn.setOnClickListener(view -> {
            descartarFotosSeleccionadas();
            ocultarDialogoConfirmacion();
        });

        noEliminarBtn.setOnClickListener(view -> {
            ocultarDialogoConfirmacion();
        });

        // 3. Cargar datos
        cargarFotosDesdeArgumentos();
    }

    // --- LÓGICA DEL DIÁLOGO ---
    private void mostrarDialogoConfirmacion() {
        if (dialogContainer != null) {
            dialogContainer.setVisibility(View.VISIBLE);
        }
    }

    private void ocultarDialogoConfirmacion() {
        if (dialogContainer != null) {
            dialogContainer.setVisibility(View.GONE);
        }
    }
    // -------------------------

    private void cargarFotosDesdeArgumentos() {
        listaFotosCamara.clear();
        Bundle args = getArguments();

        if (args != null) {
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
                        listaFotosCamara.add(fileUri);
                    }
                }
            }
        }
        adapter = new ImageAdapter(listaFotosCamara, this);
        recycler.setAdapter(adapter);
    }

    private void descartarFotosSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> discardedUris = adapter.discardSelectedItems();
        int count = discardedUris.size();

        // Eliminar físicamente los archivos del disco
        for (Uri uri : discardedUris) {
            try {
                // Intenta eliminar el archivo por su ruta (si es una Uri de FileProvider)
                File file = new File(uri.getPath());
                if (file.exists() && file.delete()) {
                    Log.d("Visualizador", "Archivo descartado eliminado: " + file.getName());
                } else {
                    requireContext().getContentResolver().delete(uri, null, null);
                }
            } catch (Exception e) {
                Log.e("Visualizador", "Error al eliminar archivo URI: " + uri.toString(), e);
            }
        }

        selectionBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), count + " fotos descartadas y eliminadas del disco.", Toast.LENGTH_SHORT).show();

        // Si no quedan fotos, regresa
        if (listaFotosCamara.isEmpty()) {
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.regresar1) {
            // Prepara el resultado con el orden actual de las fotos que NO fueron eliminadas
            Bundle result = new Bundle();
            ArrayList<String> retainedUrisStr = new ArrayList<>();

            for (Uri uri : listaFotosCamara) {
                retainedUrisStr.add(uri.toString());
            }

            result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, retainedUrisStr);

            // Envía el resultado a EscanerCifradoCamara (el fragmento anterior)
            getParentFragmentManager().setFragmentResult(KEY_REORDENAR_RESULT, result);

            // Regresa
            Navigation.findNavController(v).popBackStack();

        } else if (id == R.id.guardar) {
            if (adapter != null) {
                List<Uri> retainedUris = adapter.getRetainedItems();
                if (retainedUris.isEmpty()) {
                    Toast.makeText(getContext(), "No hay imágenes para guardar.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Guardando " + retainedUris.size() + " imágenes...", Toast.LENGTH_SHORT).show();
                    // Aquí debe ir la lógica real de guardado y cifrado
                    Navigation.findNavController(v).popBackStack();
                }
            }
        } else if (id == R.id.scancam1) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3);
        } else if (id == R.id.recortar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaCortarRotar);
        }
    }

    @Override
    public void onImageClicked(Uri uri) {
        // Lanza la actividad de visualización (VistaImagenActivity)
        ArrayList<String> uriStringList = new ArrayList<>();
        for (Uri u : listaFotosCamara) {
            uriStringList.add(u.toString());
        }

        int position = listaFotosCamara.indexOf(uri);

        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        intent.putStringArrayListExtra(VistaImagenActivity.EXTRA_URI_LIST, uriStringList);
        intent.putExtra(VistaImagenActivity.EXTRA_POSITION, position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) {
        // Actualiza la barra de selección
        if (count > 0) {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText(count + " seleccionadas");
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }
}