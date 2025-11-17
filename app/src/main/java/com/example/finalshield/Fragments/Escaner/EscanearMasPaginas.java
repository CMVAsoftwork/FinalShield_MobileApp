package com.example.finalshield.Fragments.Escaner;

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

public class EscanearMasPaginas extends Fragment implements View.OnClickListener {
    ImageButton galeria, recortar,camara, edicion, eliminar;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escanear_mas_paginas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre = v.findViewById(R.id.regresar1);
        camara = v.findViewById(R.id.scancam);
        recortar = v.findViewById(R.id.recortar);
        galeria = v.findViewById(R.id.selecgaleria);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);
        camara.setOnClickListener(this);
        recortar.setOnClickListener(this);
        galeria.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regre.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.scancam){
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        } else if (v.getId() == R.id.recortar) {
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        } else if (v.getId() == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(R.id.seleccion_imagenes);
        } else if (v.getId() == R.id.edicion) {
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (v.getId() == R.id.eliminar) {
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        }else if (v.getId() == R.id.regresar1) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
}