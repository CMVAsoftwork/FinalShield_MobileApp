package com.example.finalshield.Fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Importado para el aviso

import androidx.activity.OnBackPressedCallback; // Importado para el bloqueo
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.finalshield.R;
import com.example.finalshield.ViewModel.CargaViewModel;

public class CargaProcesos extends Fragment {

    private ImageView ivLogo;
    private TextView tvMensaje;
    private AnimatorSet animatorRespiracion;
    private CargaViewModel cargaViewModel;

    private int destinoFinal = -1;
    private Bundle argumentosDestino;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- BLOQUEO DEL BOTÓN ATRÁS (HARDWARE) ---
        // Esto intercepta el gesto o botón físico de atrás mientras este fragmento sea visible
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si el usuario intenta regresar, solo mandamos el aviso
                Toast.makeText(getContext(), "Por favor espera, estamos procesando tu solicitud de forma segura.", Toast.LENGTH_SHORT).show();
            }
        });

        if (getArguments() != null) {
            this.destinoFinal = getArguments().getInt("destino_final", -1);
            this.argumentosDestino = getArguments().getBundle("argumentos_destino");

            if (argumentosDestino != null && argumentosDestino.containsKey("security_token")) {
                Log.d("FINALSHIELD_DEBUG", "CargaProcesos: Token de seguridad recibido correctamente.");
            }
        }
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_carga_procesos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ivLogo = v.findViewById(R.id.icono_proceso);
        tvMensaje = v.findViewById(R.id.texto_carga);

        // 1. Animación de entrada suave
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(500).start();

        // 2. Iniciar animación de "respiración" infinita
        iniciarRespiracion();

        // 3. ESCUCHAR AL VIEWMODEL: Cuando el proceso termine, salimos
        cargaViewModel.finalizarCarga.observe(getViewLifecycleOwner(), terminado -> {
            if (terminado) {
                ejecutarSalidaYNav();
            }
        });
    }

    private void iniciarRespiracion() {
        if (ivLogo == null) return;
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, View.SCALE_X, 0.90f, 1.10f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, View.SCALE_Y, 0.90f, 1.10f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        animatorRespiracion = new AnimatorSet();
        animatorRespiracion.playTogether(scaleX, scaleY);
        animatorRespiracion.setDuration(1200);
        animatorRespiracion.start();
    }

    private void ejecutarSalidaYNav() {
        if (animatorRespiracion != null) animatorRespiracion.cancel();

        ObjectAnimator rotation = ObjectAnimator.ofFloat(ivLogo, View.ROTATION, 0f, 500f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, View.SCALE_X, 1f, 0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, View.SCALE_Y, 1f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLogo, View.ALPHA, 1f, 0f);

        AnimatorSet setSalida = new AnimatorSet();
        setSalida.playTogether(rotation, scaleX, scaleY, alpha);
        setSalida.setDuration(700);

        setSalida.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isAdded()) return;

                cargaViewModel.resetear();
                NavController navController = Navigation.findNavController(requireView());

                if (destinoFinal == R.id.verClave) {
                    if (argumentosDestino != null && argumentosDestino.containsKey("security_token")) {
                        navController.navigate(R.id.verClave, argumentosDestino);
                    } else {
                        Log.e("FINALSHIELD_ERROR", "Se intentó ir a verClave sin token.");
                        navController.navigate(R.id.inicio);
                    }
                }
                else if (destinoFinal != -1) {
                    navController.navigate(destinoFinal, argumentosDestino);
                } else {
                    navController.navigate(R.id.inicio);
                }
            }
        });
        setSalida.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (animatorRespiracion != null) animatorRespiracion.cancel();
    }
}