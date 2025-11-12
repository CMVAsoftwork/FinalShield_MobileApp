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

public class RegistroSesion extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre, iniises, regis;
        regre = v.findViewById(R.id.regresar2);
        iniises = v.findViewById(R.id.inisesi);
        regis = v.findViewById(R.id.regis);

        regis.setOnClickListener(this);
        regre.setOnClickListener(this);
        iniises.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.regis){
            Navigation.findNavController(v).navigate(R.id.inicio);
        } else if (v.getId() == R.id.inisesi) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        } else if (v.getId() == R.id.regresar2) {
            Navigation.findNavController(v).navigate(R.id.registroSesion);
        }
    }
}