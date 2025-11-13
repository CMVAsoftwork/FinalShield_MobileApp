package com.example.finalshield.Fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class Seleccion_imagenes extends Fragment {
    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private List<Uri> listaImagenes = new ArrayList<>();
    private static final int REQUEST_CODE = 100;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seleccion_imagenes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        recyclerViee = v.findViewById(R.id.recycler);

    }
}