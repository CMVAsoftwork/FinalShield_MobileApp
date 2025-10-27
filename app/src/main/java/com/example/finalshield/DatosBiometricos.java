package com.example.finalshield;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

public class DatosBiometricos extends Fragment implements View.OnClickListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_datos_biometricos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre;
        ImageView huella;

        regre = v.findViewById(R.id.regresar3);
        regre.setOnClickListener(this);
        huella = v.findViewById(R.id.huella);
        huella.setOnClickListener(this);;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.regresar3){
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        } else if (v.getId() == R.id.huella) {
            Navigation.findNavController(v).navigate(R.id.inicio);
        }
    }
}