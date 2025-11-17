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
import android.widget.ImageButton;

import com.example.finalshield.R;

public class OpcionCifrado extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opcion_cifrado, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button cam,gal,mixt;
        ImageButton perfil,house, archivo,candadclose, carpeta, mail, candadopen;
        perfil = v.findViewById(R.id.btnperfil);
        house = v.findViewById(R.id.house);
        archivo = v.findViewById(R.id.archivo);
        candadclose = v.findViewById(R.id.candadoclose);
        carpeta = v.findViewById(R.id.carpeta);
        mail = v.findViewById(R.id.mail);
        candadopen = v.findViewById(R.id.candadopen);
        cam = v.findViewById(R.id.cifcam);
        gal = v.findViewById(R.id.cifgal);
        mixt = v.findViewById(R.id.cifmix);
        gal.setOnClickListener(this);
        mixt.setOnClickListener(this);
        cam.setOnClickListener(this);
        perfil.setOnClickListener(this);
        house.setOnClickListener(this);
        archivo.setOnClickListener(this);
        candadclose.setOnClickListener(this);
        carpeta.setOnClickListener(this);
        mail.setOnClickListener(this);
        candadopen.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.carpeta){
            Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        } else if (v.getId() == R.id.house) {
            Navigation.findNavController(v).navigate(R.id.inicio);
        } else if (v.getId() == R.id.candadoclose) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        } else if (v.getId() == R.id.candadopen) {
            Navigation.findNavController(v).navigate(R.id.continuacionInicio);
        }else if (v.getId() == R.id.mail) {
            Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        }else if (v.getId() == R.id.archivo) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        } else if (v.getId() == R.id.btnperfil) {
            Navigation.findNavController(v).navigate(R.id.continuacionInicio);
        }else if (v.getId() == R.id.cifcam) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3);
        }else if (v.getId() == R.id.cifgal) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoGaleria2);
        }else if (v.getId() == R.id.cifmix) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        }
    }
}