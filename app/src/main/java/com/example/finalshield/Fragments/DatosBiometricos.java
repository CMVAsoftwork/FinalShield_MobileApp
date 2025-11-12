package com.example.finalshield.Fragments;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import android.widget.Toast;

import com.example.finalshield.R;

import java.util.concurrent.Executor;

public class DatosBiometricos extends Fragment implements View.OnClickListener, View.OnTouchListener {
    ImageView huella;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
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
        executor = ContextCompat.getMainExecutor(getContext());
        biometricPrompt = new BiometricPrompt(this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Código a ejecutar si hay un error (ej: el usuario cancela, muchos intentos fallidos)
                Toast.makeText(getContext(),
                        "Error de autenticación: " + errString,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Código a ejecutar si la huella ta bien
                Toast.makeText(getContext(),
                        "¡Huella verificada correctamente!",
                        Toast.LENGTH_SHORT).show();

                // Navegar a la siguiente pantalla (solo si el Fragment sigue adjunto)
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.inicio);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                // Código a ejecutar si la huella no coincide
                Toast.makeText(getContext(), "Huella no reconocida. Intente de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Inicio de Sesión Biométrico")
                .setSubtitle("Toca el sensor de huellas para acceder")
                .setNegativeButtonText("Usar Contraseña") // Necesario para cancelar
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG) // Solo huella/rostro fuerte
                .build();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.regresar3) {
            Navigation.findNavController(v).navigate(R.id.bienvenida);
        } /*else if (v.getId() == R.id.btnregis) {
            Navigation.findNavController(v).navigate(R.id.registroSesion);
        } */else if (v.getId() == R.id.btninses2) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        }
    }
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        // La animación es puramente estética y se lanza inmediatamente
        Animation animationscale = AnimationUtils.loadAnimation(getContext(), R.anim.escaladito2);
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                huella.startAnimation(animationscale);
                animationscale.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Al terminar la animación, lanzamos la autenticación biométrica
                        mostrarDialogoBiometrico();
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                    @Override
                    public void onAnimationStart(Animation animation) {}
                });
                return true; // Consumir el evento de toque
            case MotionEvent.ACTION_UP:
                return true;
            default:
                return false;
        }
    }
    private void mostrarDialogoBiometrico() {
        BiometricManager biometricManager = BiometricManager.from(getContext());
        int resultado = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (resultado == BiometricManager.BIOMETRIC_SUCCESS) {
            // Huella disponible y registrada: Iniciar el diálogo
            biometricPrompt.authenticate(promptInfo);

        } else if (resultado == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            // No hay huellas registradas: Mostrar Toast
            Toast.makeText(getContext(),
                    "Por favor, registra una huella digital en la configuración del dispositivo.",
                    Toast.LENGTH_LONG).show();

        } else {
            // Otro error (ej: hardware no disponible, no hay bloqueo de pantalla)
            Toast.makeText(getContext(),
                    "La autenticación biométrica no está disponible en este momento.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
