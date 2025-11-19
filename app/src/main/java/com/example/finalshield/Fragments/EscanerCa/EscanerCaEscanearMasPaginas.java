package com.example.finalshield.Fragments.EscanerCa;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.finalshield.R;

public class EscanerCaEscanearMasPaginas extends Fragment implements View.OnClickListener {
    ImageButton cam1, eliminar1, edicion1, recortar1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_escanear_mas_paginas, container, false);
    }
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre = v.findViewById(R.id.regresar4);
        eliminar1 = v.findViewById(R.id.eliminar1);
        cam1 = v.findViewById(R.id.scancam1);
        edicion1 = v.findViewById(R.id.edicion1);
        recortar1 = v.findViewById(R.id.recortar1);
        eliminar1.setOnClickListener(this);
        cam1.setOnClickListener(this);
        edicion1.setOnClickListener(this);
        recortar1.setOnClickListener(this);
        regre.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.eliminar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaEliminarPaginas);
        } else if (v.getId() == R.id.scancam1) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3);
        } else if (v.getId() == R.id.edicion1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaVisualizacionYReordenamiento);
        } else if (v.getId() == R.id.recortar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaCortarRotar);
        }else if (v.getId() == R.id.regresar4) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
}