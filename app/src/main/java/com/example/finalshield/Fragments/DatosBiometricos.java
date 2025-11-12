package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.example.finalshield.R;

public class DatosBiometricos extends Fragment implements View.OnClickListener, View.OnTouchListener {
    ImageView huella;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_datos_biometricos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        Button regre;
        Button regis;
        Button inic;
        regre = v.findViewById(R.id.regresar3);
        regre.setOnClickListener(this);
        regis = v.findViewById(R.id.btnregis);
        regis.setOnClickListener(this);
        inic = v.findViewById(R.id.btninses2);
        inic.setOnClickListener(this);
        huella = v.findViewById(R.id.huella);
        huella.setOnTouchListener(this);
        ;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.regresar3) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        } else if (v.getId() == R.id.btnregis) {
            Navigation.findNavController(v).navigate(R.id.registroSesion);
        } else if (v.getId() == R.id.btninses2) {
            Navigation.findNavController(v).navigate(R.id.inicio);
        }
    }
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        // 1. Cargar la animación SOLO UNA VEZ
        Animation animationscale = AnimationUtils.loadAnimation(getContext(), R.anim.escaladito2);

        // 2. Usar un switch con case (usaremos ACTION_DOWN para iniciar la animación)
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                // Iniciar la animación cuando el usuario presiona
                huella.startAnimation(animationscale);

                // Configuramos el listener para que la navegación ocurra al finalizar la animación
                animationscale.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Asegúrate de que 'v' aquí se refiere a la vista que activó el evento
                        // o usa un NavController obtenido de otra forma si 'v' no es la vista base.
                        // En este contexto de onTouch, 'v' es 'huella'.
                        Navigation.findNavController(v).navigate(R.id.inicio);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }
                });

                // Retornamos true para indicar que hemos manejado el evento de DOWN
                return true;

            case MotionEvent.ACTION_UP:
                // Opcionalmente, puedes manejar el evento ACTION_UP aquí si necesitas
                // alguna otra lógica cuando el usuario suelta.
                return true;

            // Por defecto, no manejamos otros movimientos
            default:
                return false;
        }
    }
}
