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
import android.widget.ImageButton;


public class Inicio extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ImageButton perfil,house, archivo,candadclose, carpeta, mail, candadopen;
        Button selecarpeta, correo;
        perfil = v.findViewById(R.id.btnperfil);
        house = v.findViewById(R.id.house);
        archivo = v.findViewById(R.id.archivo);
        candadclose = v.findViewById(R.id.candadoclose);
        carpeta = v.findViewById(R.id.carpeta);
        mail = v.findViewById(R.id.mail);
        candadopen = v.findViewById(R.id.candadopen);

        selecarpeta = v.findViewById(R.id.btnseleccarpeta);
        correo = v.findViewById(R.id.btnenvcorreo);

        perfil.setOnClickListener(this);
        house.setOnClickListener(this);
        archivo.setOnClickListener(this);
        candadclose.setOnClickListener(this);
        carpeta.setOnClickListener(this);
        mail.setOnClickListener(this);
        candadopen.setOnClickListener(this);

        selecarpeta.setOnClickListener(this);
        correo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.carpeta){
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        }
    }
}