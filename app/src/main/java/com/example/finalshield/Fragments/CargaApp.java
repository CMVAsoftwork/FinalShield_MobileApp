package com.example.finalshield.Fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.finalshield.R;

public class CargaApp extends Fragment {
    private static  final int tiempoEsp = 2300;
    ImageView ivalphaa;
    LinearLayout laybienv;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_carga_app, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ivalphaa = v.findViewById(R.id.icono);
        ObjectAnimator animatoralpha = ObjectAnimator.ofFloat(ivalphaa, View.ALPHA, 0.0f, 2.0f);
        animatoralpha.setDuration(tiempoEsp);
        AnimatorSet animatorsetalpha = new AnimatorSet();
        animatorsetalpha.play(animatoralpha);
        animatorsetalpha.start();
        laybienv = v.findViewById(R.id.laybienv);
        ObjectAnimator animatoralpha2 = ObjectAnimator.ofFloat(laybienv, View.ALPHA, 0.0f, 3.5f);
        animatoralpha.setDuration(tiempoEsp);
        AnimatorSet animatorsetalpha2 = new AnimatorSet();
        animatorsetalpha2.play(animatoralpha2);
        animatorsetalpha2.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    Navigation.findNavController(v).navigate(R.id.bienvenida);
                }
            }
        }, tiempoEsp);
    }
}