package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.finalshield.R;

public class Bienvenida extends Fragment implements View.OnClickListener {
    ImageView ivescala;
    int iniHu;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bienvenida, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ivescala = v.findViewById(R.id.robot);
        Animation animationscale = AnimationUtils.loadAnimation(getContext(), R.anim.escaladito);
        ivescala.startAnimation(animationscale);
        Button comenzar;
        comenzar = v.findViewById(R.id.btncomenzar);
        comenzar.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(iniHu == 1){
            Navigation.findNavController(v).navigate(R.id.datosBiometricos);
        } else if (iniHu != 1) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
            iniHu = 1;
        }
    }
}