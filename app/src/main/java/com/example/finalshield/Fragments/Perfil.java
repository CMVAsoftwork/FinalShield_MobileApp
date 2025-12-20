package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;


public class Perfil extends Fragment implements View.OnClickListener {
    TextView correo, nombre;
    Button btnCambiarContraseña, btnCerrarSesion, btnRegresar;
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
        btnRegresar = v.findViewById(R.id.regresar5);

        btnCerrarSesion.setOnClickListener(this);
        btnCambiarContraseña.setOnClickListener(this);
        btnRegresar.setOnClickListener(this);

        authService = new AuthService(requireContext());

        /*if (authService.isLoggedIn()) {
            String nombreUser = authService.obtenerNombre();
            String emailUser = authService.obtenerCorreo();

            if (nombreUser.equalsIgnoreCase("Usuario") && emailUser != null) {
                nombreUser = emailUser.split("@")[0];
            }

            nombre.setText(nombreUser);
            correo.setText(emailUser);

            correo.setSelected(true);
        }*/
        if (authService.isLoggedIn()) {
            String nombreUser = authService.obtenerNombre();
            String emailUser = authService.obtenerCorreo();

            nombre.setText(nombreUser);
            correo.setText(emailUser);
            correo.setSelected(true);
        }
    }

    @Override
    public void onClick(View v) {
        int id= v.getId();
        if (id == R.id.cerrarsecion) {
            authService.cerrarSesion();
            NavHostFragment.findNavController(this).navigate(
                    R.id.inicioSesion,
                    null,
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.naviegador, true)
                            .build()
            );
        }
        else if (id == R.id.cambiarContraseña) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_perfil2_to_cambiarContrasena);
        }
        else if (id == R.id.regresar5) {
            NavHostFragment.findNavController(this).navigate(
                    R.id.inicio,
                    null,
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.naviegador, false)
                            .build()
            );
        }
    }
}