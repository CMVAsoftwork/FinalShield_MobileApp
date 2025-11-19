package com.example.finalshield.Fragments.Escaner;

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
import androidx.core.content.ContextCompat;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class Seleccion_imagenes extends Fragment {

    private RecyclerView recycler;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenes = new ArrayList<>();
    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button clearSelection;

    private static final int REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seleccion_imagenes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recycler = v.findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));

        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        clearSelection = v.findViewById(R.id.clearSelection);

        clearSelection.setOnClickListener(view -> {
            adapter.clearSelection();
            selectionBar.setVisibility(View.GONE);
        });

        pedirPermiso();
    }

    private void pedirPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE);
        else
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            cargarImagenes();
        else
            Toast.makeText(getContext(), "Permiso requerido", Toast.LENGTH_SHORT).show();
    }

    private void cargarImagenes() {
        listaImagenes.clear();

        Uri collection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = requireActivity()
                .getContentResolver()
                .query(collection, new String[]{MediaStore.Images.Media._ID},
                        null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");

        if (cursor != null) {
            int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                listaImagenes.add(ContentUris.withAppendedId(collection, id));
            }
            cursor.close();
        }

        adapter = new ImageAdapter(listaImagenes, new ImageAdapter.Callbacks() {
            @Override
            public void onImageClicked(Uri uri) {
                Intent intent = new Intent(requireContext(), VistaImagenActivity.class);
                intent.putExtra("uri", uri.toString());
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
        });

        recycler.setAdapter(adapter);
    }
}