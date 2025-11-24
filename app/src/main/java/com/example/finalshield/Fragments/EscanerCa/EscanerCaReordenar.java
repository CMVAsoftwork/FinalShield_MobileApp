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

import android.util.Log;
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
    private View regresar;
    private View guardar;

    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        // Configurar ItemTouchHelper (tu implementación de SimpleItemTouchHelperCallback debe existir)
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);
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
                        Log.e("Reordenar", "Error al parsear URI: " + uriStr, e);
                    }
                }
            }
        }

        adapter = new ImageAdapter(listaFotosCamara, this, R.layout.item_imagen);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.regresar1 || id == R.id.guardar) {
            regresarConResultado();
        }
    }

    private void regresarConResultado() {
        Bundle result = new Bundle();
        ArrayList<String> uris = new ArrayList<>();
        List<Uri> finalUris = (adapter != null) ? adapter.getRetainedItems() : listaFotosCamara;
        for (Uri uri : finalUris) uris.add(uri.toString());
        result.putStringArrayList(BUNDLE_REORDENAR_URI_LIST, uris);
        getParentFragmentManager().setFragmentResult(KEY_REORDENAR_RESULT, result);
        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onImageClicked(Uri uri) {
        ArrayList<String> uriStringList = new ArrayList<>();
        for (Uri u : listaFotosCamara) uriStringList.add(u.toString());

        int position = listaFotosCamara.indexOf(uri);
        if (position == -1) return;

        // Inicia actividad para ver imagen
        Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
        intent.putStringArrayListExtra("uri_list", uriStringList);
        intent.putExtra("position", position);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(int count) { /* No usado en reordenar */ }
}