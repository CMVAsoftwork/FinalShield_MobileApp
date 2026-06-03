package com.example.finalshield.Fragments.InicioSesion;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.ViewModel.CargaViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Bienvenida extends Fragment {

    private AuthService authService;
    private CargaViewModel cargaViewModel;
    private boolean navegando = false;
    private Button btnComenzar;
    private ImageView ivBrazo, ivCuerpo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bienvenida, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);
        authService = new AuthService(requireContext());
        ivBrazo = v.findViewById(R.id.brazo);
        ivCuerpo = v.findViewById(R.id.robotsb);
        btnComenzar = v.findViewById(R.id.btncomenzar);
        iniciarAnimacionesRobot();
        btnComenzar.setOnClickListener(view -> {
            if (!navegando) {
                validarYDecidirDestino();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        requireActivity()
                .findViewById(R.id.fabChat)
                .setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();

        requireActivity()
                .findViewById(R.id.fabChat)
                .setVisibility(View.GONE);

        requireActivity()
                .findViewById(R.id.chatContainer)
                .setVisibility(View.GONE);
    }

    private void validarYDecidirDestino() {
        SharedPreferences linkPrefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        String pendingToken = linkPrefs.getString("pending_token", null);
        String correo = authService.obtenerCorreo();

        // Si hay un token de enlace seguro
        if (pendingToken != null) {
            if (correo == null || correo.isEmpty()) {
                navegarConCheck(R.id.inicioSesion); // Si no hay correo, debe loguearse normal
            } else {
                // IMPORTANTE: Si ya tenemos correo, vamos a biometría
                // navegarConCheck ya se encarga de meter el pendingToken en el bundle
                navegarConCheck(R.id.datosBiometricos);
            }
            return;
        }

        // Flujo normal sin link
        if (correo == null || correo.isEmpty()) {
            navegarConCheck(R.id.inicioSesion);
            return;
        }

        navegando = true;
        btnComenzar.setEnabled(false);
        btnComenzar.setText("Verificando...");
        authService.isBiometricoActivo(correo, new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (isAdded()) {
                    int destinoReal = (response.isSuccessful() && Boolean.TRUE.equals(response.body()))
                            ? R.id.datosBiometricos
                            : R.id.inicioSesion;
                    navegarConCheck(destinoReal);
                }
            }
            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                if (isAdded()) navegarConCheck(R.id.inicioSesion);
            }
        });
    }

    private void navegarConCheck(int destinoFinalId) {
        SharedPreferences linkPrefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        String pendingToken = linkPrefs.getString("pending_token", null);

        Bundle bundleCarga = new Bundle();
        bundleCarga.putInt("destino_final", destinoFinalId);

        // Empaquetamos el token en la sub-bolsa que CargaProcesos sí reenvía
        if (pendingToken != null) {
            Bundle argsFinales = new Bundle();
            argsFinales.putString("security_token", pendingToken);
            bundleCarga.putBundle("argumentos_destino", argsFinales);
        }

        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.bienvenida, true)
                .build();

        NavHostFragment.findNavController(this).navigate(R.id.cargaProcesos, bundleCarga, navOptions);
        cargaViewModel.terminarProceso();
    }

    private void iniciarAnimacionesRobot() {
        float desplazamientoY = -40f;
        int duracionFlotacion = 1500;
        if (ivCuerpo != null) {
            ObjectAnimator levitacionCuerpo = ObjectAnimator.ofFloat(ivCuerpo, "translationY", 0f, desplazamientoY);
            levitacionCuerpo.setDuration(duracionFlotacion);
            levitacionCuerpo.setRepeatCount(ValueAnimator.INFINITE);
            levitacionCuerpo.setRepeatMode(ValueAnimator.REVERSE);
            levitacionCuerpo.start();
        }
        if (ivBrazo != null) {
            ObjectAnimator levitacionBrazo = ObjectAnimator.ofFloat(ivBrazo, "translationY", 0f, desplazamientoY);
            levitacionBrazo.setDuration(duracionFlotacion);
            levitacionBrazo.setRepeatCount(ValueAnimator.INFINITE);
            levitacionBrazo.setRepeatMode(ValueAnimator.REVERSE);
            levitacionBrazo.start();
            ObjectAnimator saludo = ObjectAnimator.ofFloat(ivBrazo, "rotation", -15f, -40f);
            saludo.setDuration(400);
            saludo.setRepeatCount(ValueAnimator.INFINITE);
            saludo.setRepeatMode(ValueAnimator.REVERSE);
            saludo.start();
        }
    }
}