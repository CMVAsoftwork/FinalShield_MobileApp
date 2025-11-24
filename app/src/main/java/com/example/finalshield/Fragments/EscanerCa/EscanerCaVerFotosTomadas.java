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

    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";

    ImageButton cam1, cortar1;
    private RecyclerView recycler;
    private ImageAdapter adapter;
    private final List<Uri> listaFotosCamara = new ArrayList<>();

    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button clearSelection;
    private Button descartarSeleccion;
    private Button regresar;
    private Button guardar;

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

        dialogContainer = v.findViewById(R.id.dialogContainer);
        siEliminarBtn = v.findViewById(R.id.sieliminar);
        noEliminarBtn = v.findViewById(R.id.noeliminar);

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

        descartarSeleccion.setOnClickListener(view -> {
            if (adapter != null && adapter.getSelectedCount() > 0) mostrarDialogoConfirmacion();
        });

        siEliminarBtn.setOnClickListener(view -> {
            descartarFotosSeleccionadas();
            ocultarDialogoConfirmacion();
        });

        noEliminarBtn.setOnClickListener(view -> ocultarDialogoConfirmacion());

        cargarFotosDesdeArgumentos();
    }

    private void mostrarDialogoConfirmacion() {
        if (dialogContainer != null) dialogContainer.setVisibility(View.VISIBLE);
    }

    private void ocultarDialogoConfirmacion() {
        if (dialogContainer != null) dialogContainer.setVisibility(View.GONE);
    }

    private void cargarFotosDesdeArgumentos() {
        listaFotosCamara.clear();
        Bundle args = getArguments();

        if (args != null) {
            ArrayList<String> uriStrings = args.getStringArrayList("FOTOS_CAPTURA");
            if (uriStrings != null) {
                for (String uriStr : uriStrings) {
                    try {
                        Uri fileUri = Uri.parse(uriStr);
                        listaFotosCamara.add(fileUri);
                    } catch (Exception e) {
                        Log.e("VerFotosTomadas", "Error al parsear URI: " + uriStr, e);
                    }
                }
            }
        }

        adapter = new ImageAdapter(listaFotosCamara, this, R.layout.item_imagen, true);
        recycler.setAdapter(adapter);
    }

    private void descartarFotosSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> discardedUris = adapter.discardSelectedItems();
        int count = discardedUris.size();

        for (Uri uri : discardedUris) {
            try {
                String fileName = uri.getLastPathSegment();
                if (fileName != null) {
                    File file = new File(requireContext().getCacheDir(), fileName);
                    if (file.exists() && file.delete()) {
                        Log.d("Visualizador", "Archivo descartado eliminado: " + file.getName());
                    } else {
                        requireContext().getContentResolver().delete(uri, null, null);
                    }
                }
            } catch (Exception e) {
                Log.e("Visualizador", "Error al eliminar archivo URI: " + uri.toString(), e);
            }
        }

        selectionBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), count + " fotos descartadas y eliminadas del disco.", Toast.LENGTH_SHORT).show();

        if (listaFotosCamara.isEmpty()) {
            regresarA_EscanerCifradoCamara();
        }
    }

    private void regresarA_EscanerCifradoCamara() {
        Bundle result = new Bundle();
        ArrayList<String> retainedUrisStr = new ArrayList<>();
        for (Uri uri : listaFotosCamara) retainedUrisStr.add(uri.toString());
        result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, retainedUrisStr);

        getParentFragmentManager().setFragmentResult(KEY_REORDENAR_RESULT, result);
        // volvemos atrás en el stack
        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Bundle bundle = new Bundle();
        ArrayList<String> filePaths = new ArrayList<>();
        for (Uri uri : listaFotosCamara) filePaths.add(uri.toString());
        bundle.putStringArrayList("FOTOS_CAPTURA", filePaths);

        if (id == R.id.regresar1) {
            regresarA_EscanerCifradoCamara();
        } else if (id == R.id.guardar) {
            regresarA_EscanerCifradoCamara();
        } else if (id == R.id.scancam1) {
            regresarA_EscanerCifradoCamara();
        } else if (id == R.id.recortar1) {
            if (!listaFotosCamara.isEmpty()) {
                Navigation.findNavController(v).navigate(R.id.escanerCaCortarRotar, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para editar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onImageClicked(Uri uri) {
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
    public void onSelectionChanged(int count) {
        if (count > 0) {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText(count + " seleccionadas");
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }
}