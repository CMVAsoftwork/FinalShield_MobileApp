package com.example.finalshield.Fragments.EscanerGa;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class EscanerGaCortarRotar extends Fragment implements View.OnClickListener, ImageAdapter.Callbacks {
    ImageButton selecimg2, edicion2, eliminar2;
    private RecyclerView recycler;
    private Button regresar;
    private SharedImageViewModel sharedViewModel;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenesGaleria = new ArrayList<>(); // Lista local con los elementos seleccionados

    // Claves para el FragmentResult
    // Usaremos esta clave en EscanerCifradoGaleria para recibir la lista actualizada.
    public static final String KEY_ACTUALIZACION_LISTA = "eliminacion_lista_actualizada";
    public static final String BUNDLE_RESULT_URI_LIST = "resultado_uri_list";
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ga_cortar_rotar, container, false);
    }
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        regresar = v.findViewById(R.id.regresar3);
        selecimg2 = v.findViewById(R.id.selecgaleria2);
        edicion2 = v.findViewById(R.id.edicion2);
        eliminar2 = v.findViewById(R.id.eliminar2);
        recycler = v.findViewById(R.id.imgsrecortarG);


        selecimg2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        regresar.setOnClickListener(this);


        cargarFotosDesdeViewModel();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.selecgaleria2) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoGaleria2);
        } else if (v.getId() == R.id.edicion2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);
        } else if (v.getId() == R.id.eliminar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);
        }else if (v.getId() == R.id.regresar3) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
    private void cargarFotosDesdeViewModel() {
        List<Uri> selectedUris = sharedViewModel.getImageUriList();

        if (selectedUris.isEmpty()) {
            Toast.makeText(getContext(), "No hay fotos seleccionadas para recortar.", Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        listaImagenesGaleria.clear();
        listaImagenesGaleria.addAll(selectedUris);

        // Limpiar el ViewModel inmediatamente después de obtener la lista
        sharedViewModel.clearList();

        adapter = new ImageAdapter(listaImagenesGaleria, this, R.layout.item_imagen, true);
        recycler.setAdapter(adapter);
    }

    @Override
    public void onImageClicked(Uri uri) {

    }

    @Override
    public void onSelectionChanged(int count) {

    }
}