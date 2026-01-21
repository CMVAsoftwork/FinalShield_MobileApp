package com.example.finalshield.Fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finalshield.R;

public class CargaProcesos extends Fragment {

    private ImageView ivLogo;
    private TextView tvMensaje;
    private AnimatorSet animatorRespiracion;

    // ID del destino final
    private int destinoFinal = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_carga_procesos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // 1. Recuperar el destino del Bundle (si existe)
        if (getArguments() != null) {
            destinoFinal = getArguments().getInt("destino_final", -1);
        }

        ivLogo = v.findViewById(R.id.icono_proceso);
        tvMensaje = v.findViewById(R.id.texto_carga);

        // Animación de entrada
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(800).start();

        // Configuración de animación de respiración
        if (ivLogo != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, View.SCALE_X, 0.95f, 1.05f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, View.SCALE_Y, 0.95f, 1.05f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLogo, View.ALPHA, 0.8f, 1.0f);

            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setRepeatMode(ValueAnimator.REVERSE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatMode(ValueAnimator.REVERSE);
            alpha.setRepeatCount(ValueAnimator.INFINITE);
            alpha.setRepeatMode(ValueAnimator.REVERSE);

            animatorRespiracion = new AnimatorSet();
            animatorRespiracion.playTogether(scaleX, scaleY, alpha);
            animatorRespiracion.setDuration(1500);
            animatorRespiracion.start();
        }

        // 2. DISPARADOR DE NAVEGACIÓN
        // Usamos un delay para que la animación se aprecie y luego navegue
        new Handler(Looper.getMainLooper()).postDelayed(this::finalizarYSalir, 3000);
    }

    private void finalizarYSalir() {
        if (!isAdded()) return;

        // Si tenemos un destino enviado desde ArchivosDescifrados, vamos allá
        if (destinoFinal != -1) {
            Navigation.findNavController(requireView()).navigate(destinoFinal);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (animatorRespiracion != null) {
            animatorRespiracion.cancel();
        }
    }
}