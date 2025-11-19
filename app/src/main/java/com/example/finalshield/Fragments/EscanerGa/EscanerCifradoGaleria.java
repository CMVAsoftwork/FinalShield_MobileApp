package com.example.finalshield.Fragments.EscanerGa;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class EscanerCifradoGaleria extends Fragment implements View.OnClickListener {

    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenes2 = new ArrayList<>();

    // Barra de selección
    private RelativeLayout selectionBar;
    private TextView selectionCount;
    private ImageButton clearSelection;

    // Botones inferiores
    ImageButton addele2, recortar2, edicion2, eliminar2;

    private static final int REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_galeria, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recyclerViee = v.findViewById(R.id.recycler2);
        recyclerViee.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // === barra selección ===
        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        clearSelection = v.findViewById(R.id.clearSelection);

        clearSelection.setOnClickListener(view -> {
            adapter.clearSelection();
            actualizarBarra();
        });

        // === botones inferiores ===
        addele2 = v.findViewById(R.id.addelements2);
        recortar2 = v.findViewById(R.id.recortar2);
        edicion2 = v.findViewById(R.id.edicion2);
        eliminar2 = v.findViewById(R.id.eliminar2);

        addele2.setOnClickListener(this);
        recortar2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);

        // === botón regresar ===
        Button regresar = v.findViewById(R.id.regresar3);
        regresar.setOnClickListener(this);

        // === botón guardar ===
        Button guardar = v.findViewById(R.id.guardar3);
        guardar.setOnClickListener(this);

        pedirPermiso();
    }

    private void pedirPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE);
        else
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            cargarImagenes();

        } else {
            Toast.makeText(getContext(), "Permiso requerido", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarImagenes() {
        listaImagenes2.clear();

        Uri collection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = requireActivity().getContentResolver().query(
                collection,
                new String[]{MediaStore.Images.Media._ID},
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                listaImagenes2.add(ContentUris.withAppendedId(collection, id));
            }
            cursor.close();
        }

        adapter = new ImageAdapter(listaImagenes2, new ImageAdapter.Callbacks() {

            @Override
            public void onImageClicked(Uri uri) {
                if (!adapter.isSelectionMode()) {
                    Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
                    intent.putExtra("uri", uri.toString());
                    startActivity(intent);
                }
            }

            @Override
            public void onSelectionChanged(int count) {
                actualizarBarra();
            }
        });

        recyclerViee.setAdapter(adapter);
    }

    private void actualizarBarra() {
        int count = adapter.getSelectedCount();

        if (count == 0) {
            selectionBar.setVisibility(View.GONE);
        } else {
            selectionBar.setVisibility(View.VISIBLE);
            selectionCount.setText(count + " seleccionadas");
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.addelements2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEscanearMasPaginas);

        } else if (id == R.id.recortar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);

        } else if (id == R.id.edicion2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);

        } else if (id == R.id.eliminar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);

        } else if (id == R.id.regresar3) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
}