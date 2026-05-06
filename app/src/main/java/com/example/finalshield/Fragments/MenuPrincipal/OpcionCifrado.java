package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.finalshield.R;

import java.util.concurrent.Executor;

public class OpcionCifrado extends Fragment implements View.OnClickListener {

    private int intentosHuella = 0;
    private static final int MAX_INTENTOS = 3;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opcion_cifrado, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Botones que requieren Biometría
        v.findViewById(R.id.cifcam).setOnClickListener(this);
        v.findViewById(R.id.cifgal).setOnClickListener(this);
        v.findViewById(R.id.cifmix).setOnClickListener(this);

        // Botones de Navegación Inferior (Directos)
        v.findViewById(R.id.btnperfil).setOnClickListener(this);
        v.findViewById(R.id.house).setOnClickListener(this);
        v.findViewById(R.id.archivo).setOnClickListener(this);
        v.findViewById(R.id.candadoclose).setOnClickListener(this);
        v.findViewById(R.id.carpeta).setOnClickListener(this);
        v.findViewById(R.id.mail).setOnClickListener(this);
        v.findViewById(R.id.candadopen).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Validamos si la opción clickeada requiere protección
        if (id == R.id.cifcam || id == R.id.cifgal || id == R.id.cifmix) {
            verificarHuellaParaNavegar(v, id);
        } else {
            // Navegación normal sin huella
            gestionarNavegacionDirecta(v, id);
        }
    }

    private void verificarHuellaParaNavegar(View v, int destinationId) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                intentosHuella = 0;
                ejecutarNavegacionEscaneo(v, destinationId);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);

                // FILTRO DE CANCELACIÓN:
                // Si el código es por cancelación del usuario, NO navegamos a inicio.
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    return; // Se queda en OpcionCifrado
                }

                // Si es por otra razón (como bloqueo tras muchos intentos en el sistema)
                Toast.makeText(getContext(), "Error: " + errString, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(v).navigate(R.id.inicio);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                intentosHuella++;
                if (intentosHuella >= MAX_INTENTOS) {
                    Toast.makeText(getContext(), "Demasiados intentos fallidos", Toast.LENGTH_LONG).show();
                    intentosHuella = 0;
                    Navigation.findNavController(v).navigate(R.id.inicio);
                }
                // Si falla pero no ha llegado a 3, el diálogo suele quedarse abierto
                // o permitir reintentar automáticamente.
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso Seguro - FinalShield")
                .setSubtitle("Usa tu huella o PIN para continuar")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .setConfirmationRequired(false)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void gestionarNavegacionDirecta(View v, int id) {
        if (id == R.id.carpeta) {
            Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        } else if (id == R.id.house) {
            Navigation.findNavController(v).navigate(R.id.inicio);
        } else if (id == R.id.candadoclose) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        } else if (id == R.id.candadopen) {
            Navigation.findNavController(v).navigate(R.id.filtroDescifrados);
        } else if (id == R.id.mail) {
            Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        } else if (id == R.id.archivo) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        } else if (id == R.id.btnperfil) {
            Navigation.findNavController(v).navigate(R.id.perfil2);
        }
    }

    private void ejecutarNavegacionEscaneo(View v, int id) {
        if (id == R.id.cifcam) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoCamara3);
        } else if (id == R.id.cifgal) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoGaleria2);
        } else if (id == R.id.cifmix) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        }
    }
}