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
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private RecyclerView recycler;
    private ImageAdapter adapter;
    private final List<Uri> listaFotosCamara = new ArrayList<>();

    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button clearSelection;
    private Button descartarSeleccion;
    private Button regresar;
    private Button guardar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_ver_fotos_tomadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        regresar = v.findViewById(R.id.regresar1);
        guardar = v.findViewById(R.id.guardar);
        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        clearSelection = v.findViewById(R.id.clearSelection);
        descartarSeleccion = v.findViewById(R.id.descartarSeleccion);

        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        clearSelection.setOnClickListener(view -> {
            if (adapter != null) {
                adapter.clearSelection();
                selectionBar.setVisibility(View.GONE);
            }
        });

        descartarSeleccion.setOnClickListener(view -> {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                descartarFotosSeleccionadas();
            }
        });

        cargarFotosDesdeArgumentos();
    }

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
                // Obtener el archivo y eliminarlo directamente.
                File file = new File(uri.getPath());
                if (file.exists() && file.delete()) {
                    Log.d("Visualizador", "Archivo descartado eliminado: " + file.getName());
                } else {
                    // Intento con ContentResolver como fallback
                    requireContext().getContentResolver().delete(uri, null, null);
                }
            } catch (Exception e) {
                Log.e("Visualizador", "Error al eliminar archivo URI: " + uri.toString(), e);
            }
        }

        selectionBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), count + " fotos descartadas y eliminadas del disco.", Toast.LENGTH_SHORT).show();

        if (listaFotosCamara.isEmpty()) {
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.regresar1) {
            Navigation.findNavController(v).popBackStack();
        } else if (id == R.id.guardar) {
            if (adapter != null) {
                List<Uri> retainedUris = adapter.getRetainedItems(); // Obtiene las fotos que quedaron

                if (retainedUris.isEmpty()) {
                    Toast.makeText(getContext(), "No hay imágenes para guardar.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Guardando " + retainedUris.size() + " imágenes...", Toast.LENGTH_SHORT).show();

                    // logica del guardado y cifrado
                    Navigation.findNavController(v).popBackStack();
                }
            }
        }
    }

    // lanza la actividad de visualización
    @Override
    public void onImageClicked(Uri uri) {
        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        // pasamos la URI como String
        intent.putExtra("uri", uri.toString());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    // actualiza el contador de la barra de selección
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