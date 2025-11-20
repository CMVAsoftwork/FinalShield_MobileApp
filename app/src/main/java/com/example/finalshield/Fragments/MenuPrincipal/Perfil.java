package com.example.finalshield.Fragments.MenuPrincipal;

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

public class Perfil extends Fragment implements View.OnClickListener {
    Button regre,cerrarsesi,cambiarcontra;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        regre = v.findViewById(R.id.regresar5);
        cerrarsesi = v.findViewById(R.id.cerrarsecion);
        regre.setOnClickListener(this);
        cerrarsesi.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.regresar5){
            Navigation.findNavController(v).navigateUp();
        }
    }
}