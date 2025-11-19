package com.example.finalshield.Fragments.EscanerGa;

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


public class EscanerGaVisualizacionYReordenamiento extends Fragment implements View.OnClickListener {
    ImageButton selecimg2, addele2, eliminar2, recortar2;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_ga_visualizacion_y_reordenamiento, container, false);
    }
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre = v.findViewById(R.id.regresar3);
        addele2 = v.findViewById(R.id.addelements2);
        selecimg2 = v.findViewById(R.id.selecgaleria2);
        eliminar2 = v.findViewById(R.id.eliminar2);
        recortar2 = v.findViewById(R.id.recortar2);
        addele2.setOnClickListener(this);
        selecimg2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        recortar2.setOnClickListener(this);
        regre.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.addelements2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEscanearMasPaginas);
        } else if (v.getId() == R.id.selecgaleria2) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoGaleria2);
        } else if (v.getId() == R.id.eliminar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);
        } else if (v.getId() == R.id.recortar2) {
            Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
        }else if (v.getId() == R.id.regresar3) {
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
    }
}