package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;


public class Perfil extends Fragment implements View.OnClickListener {
    TextView correo, nombre;
    Button btnCambiarContraseña, btnCerrarSesion;
    AuthService authService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        correo = v.findViewById(R.id.correoUsuario);
        nombre = v.findViewById(R.id.nombreUsuario);
        btnCambiarContraseña = v.findViewById(R.id.cambiarContraseña);
        btnCerrarSesion = v.findViewById(R.id.cerrarsecion);

        btnCerrarSesion.setOnClickListener(this);
        btnCambiarContraseña.setOnClickListener(this);
        authService = new AuthService(requireContext());

    }

    @Override
    public void onClick(View v) {
        int id= v.getId();
        if (id == R.id.cerrarsecion){
            authService.cerrarSesion();
        } else if (id == R.id.cambiarContraseña) {


        }

    }
}