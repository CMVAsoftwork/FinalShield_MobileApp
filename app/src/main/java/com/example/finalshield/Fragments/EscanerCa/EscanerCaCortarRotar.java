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

public class EscanerCaCortarRotar extends Fragment implements View.OnClickListener {
    ImageButton cam1, addele1, edicion1, eliminar1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ca_cortar_rotar, container, false);
    }
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre = v.findViewById(R.id.regresar4);
        addele1 = v.findViewById(R.id.addelements1);
        cam1 = v.findViewById(R.id.scancam1);
        edicion1 = v.findViewById(R.id.edicion1);
        eliminar1 = v.findViewById(R.id.eliminar1);
        addele1.setOnClickListener(this);
        cam1.setOnClickListener(this);
        edicion1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);
        regre.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.addelements1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaEscanearMasPaginas);
        } else if (v.getId() == R.id.scancam1) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3);
        } else if (v.getId() == R.id.edicion1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaVisualizacionYReordenamiento);
        } else if (v.getId() == R.id.eliminar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaEliminarPaginas);
        }else if (v.getId() == R.id.regresar4) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
}