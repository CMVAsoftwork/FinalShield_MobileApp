package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.finalshield.R;

public class InicioSesion extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre, inises, regis;
        regre = v.findViewById(R.id.regresar1);
        regis = v.findViewById(R.id.btnregis);
        inises = v.findViewById(R.id.btninises1);
        regre.setOnClickListener(this);
        regis.setOnClickListener(this);
        inises.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.regresar1){
            Navigation.findNavController(v).navigate(R.id.bienvenida);
        } else if (v.getId() == R.id.btninises1) {
            Navigation.findNavController(v).navigate(R.id.datosBiometricos);
        } else if (v.getId() == R.id.btnregis) {
            Navigation.findNavController(v).navigate(R.id.registroSesion);
        }
    }
}