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
        // 1. REVISAR EL DEEP LINK PRIMERO QUE NADA
        SharedPreferences linkPrefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        String pendingToken = linkPrefs.getString("pending_token", null);

        // DEBUG PARA QUE VEAS EN CONSOLA SI EL TOKEN LLEGA AQUÍ
        Log.d("FINALSHIELD_FLOW", "Token detectado en Biometricos: " + (pendingToken != null));

        // 2. DEFINIR DESTINO (Si hay token, VerClave es OBLIGATORIO)
        final int destinoId = (pendingToken != null) ? R.id.verClave : R.id.inicio;

        final Bundle bundleCarga = new Bundle();
        bundleCarga.putInt("destino_final", destinoId);

        if (pendingToken != null) {
            Bundle argsDestino = new Bundle();
            argsDestino.putString("security_token", pendingToken);
            bundleCarga.putBundle("argumentos_destino", argsDestino);

            // Limpiamos el token para que no se cicle en el futuro
            linkPrefs.edit().remove("pending_token").apply();
        }

        // 3. SEGUIR CON EL LOGIN Y NAVEGAR
        authService.loginBiometrico(correoParaLogin, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful()) {
                    // ESTA ES LA CLAVE: Pasar el bundleCarga que configuramos arriba
                    NavHostFragment.findNavController(DatosBiometricos.this)
                            .navigate(R.id.cargaProcesos, bundleCarga);

                    cargaViewModel.terminarProceso();
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