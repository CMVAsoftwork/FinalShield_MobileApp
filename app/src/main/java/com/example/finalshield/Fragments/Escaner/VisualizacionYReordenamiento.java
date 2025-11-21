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

public class VisualizacionYReordenamiento extends Fragment implements View.OnClickListener{
    ImageButton galeria,camara, recortar, eliminar;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_visualizacion_y_reordenamiento, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre = v.findViewById(R.id.regresar1);
        camara = v.findViewById(R.id.scancam);
        galeria = v.findViewById(R.id.selecgaleria);
        recortar = v.findViewById(R.id.recortar);
        eliminar = v.findViewById(R.id.eliminar);
        camara.setOnClickListener(this);
        galeria.setOnClickListener(this);
        recortar.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regre.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.scancam){
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        } else if (v.getId() == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(R.id.seleccion_imagenes);
        } else if (v.getId() == R.id.recortar) {
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        } else if (v.getId() == R.id.eliminar) {
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        } else if (v.getId() == R.id.regresar1) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
}