package com.example.finalshield.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Perfil extends Fragment implements View.OnClickListener {

    private TextView correo, nombre;
    private Button btnCambiarContraseña, btnCerrarSesion, btnRegresar, btnMiActividad;
    private SwitchCompat switchHuella;

    private AuthService authService;
    private CargaViewModel cargaViewModel; // El control de la animación
    private SharedPreferences preferences;
    private int intentosHuella = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        authService = new AuthService(requireContext());
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);
        preferences = requireContext().getSharedPreferences("finalshield_prefs", Context.MODE_PRIVATE);

        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        correo = v.findViewById(R.id.correoUsuario);
        nombre = v.findViewById(R.id.nombreUsuario);
        btnCambiarContraseña = v.findViewById(R.id.cambiarContraseña);
        btnCerrarSesion = v.findViewById(R.id.cerrarsecion);
        btnRegresar = v.findViewById(R.id.regresar5);
        switchHuella = v.findViewById(R.id.switchHuellaInicio);
        btnMiActividad = v.findViewById(R.id.miactividad);

        btnCerrarSesion.setOnClickListener(this);
        btnCambiarContraseña.setOnClickListener(this);
        btnRegresar.setOnClickListener(this);
        btnMiActividad.setOnClickListener(this);

        if (authService.isLoggedIn()) {
            nombre.setText(authService.obtenerNombre());
            String emailUsuario = authService.obtenerCorreo();
            correo.setText(emailUsuario);

            boolean huellaActiva = preferences.getBoolean("huella_login", false);
            switchHuella.setChecked(huellaActiva);

            switchHuella.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    actualizarEstadoBiometricoEnServidor(emailUsuario, isChecked);
                }
            });
        }
    }

    private void actualizarEstadoBiometricoEnServidor(String email, boolean nuevoEstado) {
        switchHuella.setEnabled(false);
        authService.habilitarBiometrico(email, nuevoEstado, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (isAdded()) {
                    switchHuella.setEnabled(true);
                    if (response.isSuccessful()) {
                        preferences.edit().putBoolean("huella_login", nuevoEstado).apply();
                        Toast.makeText(getContext(), "Configuración actualizada", Toast.LENGTH_SHORT).show();
                    } else {
                        switchHuella.setChecked(!nuevoEstado);
                        Toast.makeText(getContext(), "Error en servidor", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (isAdded()) {
                    switchHuella.setEnabled(true);
                    switchHuella.setChecked(!nuevoEstado);
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = NavHostFragment.findNavController(this);

        if (id == R.id.cerrarsecion) {
            solicitarHuellaParaCerrarSesion();
        } else if (id == R.id.cambiarContraseña) {
            nav.navigate(R.id.action_perfil2_to_cambiarContrasena);
        } else if (id == R.id.regresar5) {
            nav.popBackStack();
        } else if (id == R.id.miactividad) {
            nav.navigate(R.id.action_perfil2_to_analisis_Datos);
        }
    }

    private void solicitarHuellaParaCerrarSesion() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                intentosHuella = 0;

                // Navegamos a la carga PRIMERO
                Bundle bundleCarga = new Bundle();
                bundleCarga.putInt("destino_final", R.id.inicioSesion);
                Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, bundleCarga);

                // Ejecutamos el cierre y avisamos a la carga que termine
                iniciarCierreSesionLimpio();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    validarIntentosSesion();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                validarIntentosSesion();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirmar Salida")
                .setSubtitle("Usa tu huella para cerrar sesión de forma segura")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void iniciarCierreSesionLimpio() {
        // Limpiamos token y datos locales
        authService.cerrarSesion();

        // Un pequeño delay artificial de 800ms para que se aprecie el logo de FinalShield
        // y el usuario sienta que se están "limpiando" los datos.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() || !isDetached()) {
                cargaViewModel.terminarProceso();
            }
        }, 800);
    }

    private void validarIntentosSesion() {
        intentosHuella++;
        if (intentosHuella >= 5) {
            Toast.makeText(getContext(), "Bloqueo por seguridad.", Toast.LENGTH_LONG).show();
            requireActivity().finishAffinity();
        } else {
            Toast.makeText(getContext(), "Error (" + intentosHuella + "/5)", Toast.LENGTH_SHORT).show();
        }
    }
}