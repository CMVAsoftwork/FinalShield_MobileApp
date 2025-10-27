package com.example.finalshield;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class CargaApp extends Fragment {
    private static  final int tiempoEsp = 2000;
    //ImageView ivscalee;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_carga_app, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        /*ivscalee = v.findViewById(R.id.icono);
        Animation animationscale = AnimationUtils.loadAnimation(requireContext(), R.anim.escaladito);
        ivscalee.startAnimation(animationscale);*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Navigation.findNavController(v).navigate(R.id.bienvenida);
            }
        },tiempoEsp);
    }
}