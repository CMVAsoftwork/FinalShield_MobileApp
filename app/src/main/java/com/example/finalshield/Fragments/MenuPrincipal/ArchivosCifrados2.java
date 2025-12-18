package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.finalshield.R;

public class ArchivosCifrados2 extends Fragment implements View.OnClickListener{
    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleSelectedUri(uri);
                } else {
                    Toast.makeText(getContext(), "Selección de archivo cancelada", Toast.LENGTH_SHORT).show();
                }
            });
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_cifrados2, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        ImageButton perfil,house, archivo,candadclose, carpeta, mail, candadopen;
        Button escanarch;
        escanarch =v.findViewById(R.id.btnescanycifrar);
        perfil = v.findViewById(R.id.btnperfil);
        house = v.findViewById(R.id.house);
        archivo = v.findViewById(R.id.archivo);
        candadclose = v.findViewById(R.id.candadoclose);
        carpeta = v.findViewById(R.id.carpeta);
        mail = v.findViewById(R.id.mail);
        candadopen = v.findViewById(R.id.candadopen);
        perfil.setOnClickListener(this);
        house.setOnClickListener(this);
        archivo.setOnClickListener(this);
        candadclose.setOnClickListener(this);
        carpeta.setOnClickListener(this);
        mail.setOnClickListener(this);
        candadopen.setOnClickListener(this);
        escanarch.setOnClickListener(this);
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
            Navigation.findNavController(v).navigate(R.id.archivosDesifrados);
        } else if (v.getId() == R.id.mail) {
            Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        }else if (v.getId() == R.id.archivo) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        } else if (v.getId() == R.id.btnperfil) {
            Navigation.findNavController(v).navigate(R.id.perfil);
        }else if (v.getId() == R.id.btnescanycifrar){
            filePickerLauncher.launch("*/*");
        }
    }
    private void handleSelectedUri(Uri uri) {
        Toast.makeText(getContext(), "Archivo seleccionado: " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();
    }
}