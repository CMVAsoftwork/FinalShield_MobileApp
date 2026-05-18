package com.example.finalshield.Fragments.InicioSesion;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.util.concurrent.Executor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DatosBiometricos extends Fragment implements View.OnClickListener, View.OnTouchListener {

    private ImageView huella;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private AuthService authService;
    private CargaViewModel cargaViewModel;
    private String correoParaLogin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_datos_biometricos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        authService = new AuthService(requireContext());
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);
        correoParaLogin = authService.obtenerCorreo();

        v.findViewById(R.id.regresar3).setOnClickListener(this);
        v.findViewById(R.id.btnregis).setOnClickListener(this);
        v.findViewById(R.id.btninses2).setOnClickListener(this);
        huella = v.findViewById(R.id.huella);
        huella.setOnTouchListener(this);

        configurarBiometria();
    }

    private void configurarBiometria() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                lanzarFlujoDeCarga();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinalShield")
                .setSubtitle("Identidad verificada por hardware")
                .setNegativeButtonText("Usar contraseña")
                .build();
    }

    private void lanzarFlujoDeCarga() {
        // 1. LEER EL RECADO DIRECTO DE LA MAIN_ACTIVITY (DEEP LINK DE TRABAJO ASÍNCRONO)
        com.example.finalshield.MainActivity activity = (com.example.finalshield.MainActivity) requireActivity();
        String desvioNotificacion = activity.getDestinoPendiente();

        // 2. REVISAR SI EXISTÍA OTRO DEEP LINK WEB (TU TOKEN ANTERIOR)
        SharedPreferences linkPrefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        String pendingToken = linkPrefs.getString("pending_token", null);

        Log.d("FINALSHIELD_FLOW", "Notificación pendiente: " + desvioNotificacion + " | Token Web pendiente: " + (pendingToken != null));

        // 3. DISCRIMINACIÓN DE DESTINO DE NAVEGACIÓN INTELIGENTE
        int destinoId = R.id.inicio; // Fallback por defecto si se abre la app normal

        if ("CIFRADOS".equals(desvioNotificacion)) {
            destinoId = R.id.archivosCifrados2;
            activity.limpiarDestinoPendiente(); // Consumimos el recado de inmediato
        } else if ("DESCIFRADOS".equals(desvioNotificacion)) {
            destinoId = R.id.filtroDescifrados;
            activity.limpiarDestinoPendiente();
        } else if (pendingToken != null) {
            destinoId = R.id.verClave; // Mantiene compatibilidad con tu flujo de tokens anterior
        }

        final Bundle bundleCarga = new Bundle();
        bundleCarga.putInt("destino_final", destinoId);

        // Adjuntar argumentos del token web si existían
        if (pendingToken != null && destinoId == R.id.verClave) {
            Bundle argsDestino = new Bundle();
            argsDestino.putString("security_token", pendingToken);
            bundleCarga.putBundle("argumentos_destino", argsDestino);
            linkPrefs.edit().remove("pending_token").apply();
        }

        // 4. EJECUTAR LOGIN BIOMÉTRICO CONTRA LA API
        authService.loginBiometrico(correoParaLogin, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    // Catapultamos a CargaProcesos con las órdenes de destino finales inyectadas
                    if (isAdded()) {
                        NavHostFragment.findNavController(DatosBiometricos.this)
                                .navigate(R.id.cargaProcesos, bundleCarga);
                        cargaViewModel.terminarProceso();
                    }
                } else {
                    manejarErrorAuth("Error de servidor");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                manejarErrorAuth("Error de red");
            }
        });
    }
    private void manejarErrorAuth(String msj) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                cargaViewModel.resetear();
                NavHostFragment.findNavController(this).popBackStack();
                Toast.makeText(getContext(), msj, Toast.LENGTH_SHORT).show();
            }
        }, 800);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            huella.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.escaladito2));
            BiometricManager bm = BiometricManager.from(requireContext());
            if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                biometricPrompt.authenticate(promptInfo);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.regresar3) NavHostFragment.findNavController(this).navigate(R.id.bienvenida);
        else if (id == R.id.btninses2) NavHostFragment.findNavController(this).navigate(R.id.inicioSesion);
        else if (id == R.id.btnregis) startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
    }
}