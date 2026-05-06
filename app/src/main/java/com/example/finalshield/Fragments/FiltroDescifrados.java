package com.example.finalshield.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.ViewModel.CargaViewModel; // Verifica que esta ruta sea correcta

import java.util.concurrent.Executor;

public class FiltroDescifrados extends Fragment implements View.OnClickListener {

    private EditText[] digitos;
    private int intentosFallidosPin = 0;
    private int intentosFallidosHuella = 0;
    private final int MAX_INTENTOS_PIN = 3;
    private final int MAX_INTENTOS_HUELLA = 5;
    private AuthService authService;
    private CargaViewModel cargaViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authService = new AuthService(requireContext());
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.inicio);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filtro_descifrados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        digitos = new EditText[]{
                v.findViewById(R.id.digito1), v.findViewById(R.id.digito2),
                v.findViewById(R.id.digito3), v.findViewById(R.id.digito4),
                v.findViewById(R.id.digito5), v.findViewById(R.id.digito6)
        };

        configurarPasadoAutomatico();

        int[] navIds = {R.id.house, R.id.candadoclose, R.id.carpeta, R.id.archivo, R.id.mail, R.id.btnperfil};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        digitos[0].requestFocus();
    }

    private void configurarPasadoAutomatico() {
        for (int i = 0; i < digitos.length; i++) {
            final int index = i;
            digitos[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < digitos.length - 1) {
                        digitos[index + 1].requestFocus();
                    } else if (s.length() == 1 && index == digitos.length - 1) {
                        validarPin();
                    }
                }
            });

            digitos[i].setOnKeyListener((view, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (digitos[index].getText().toString().isEmpty() && index > 0) {
                        digitos[index - 1].requestFocus();
                    }
                }
                return false;
            });
        }
    }

    private void validarPin() {
        StringBuilder pinBuilder = new StringBuilder();
        for (EditText et : digitos) pinBuilder.append(et.getText().toString());
        String pinIngresado = pinBuilder.toString();

        String pinReal = authService.obtenerPinReal();
        String pinSeguro = authService.obtenerPinSeguro();

        if (pinIngresado.equals(pinReal)) {
            intentosFallidosPin = 0;
            solicitarHuella(R.id.archivosDesifrados, "Acceso Autorizado");
        } else if (pinIngresado.equals(pinSeguro)) {
            intentosFallidosPin = 0;
            solicitarHuella(R.id.archivosDSeguros, "Bóveda de Seguridad");
        } else {
            manejarErrorPin();
        }
    }

    private void solicitarHuella(int destinoId, String mensajeExito) {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                intentosFallidosHuella = 0;

                // 1. Ocultamos teclado para evitar lag gráfico
                ocultarTeclado();

                // 2. Navegamos a CargaProcesos inmediatamente
                Bundle args = new Bundle();
                args.putInt("destino_final", destinoId);

                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.filtroDescifrados, true)
                        .build();

                Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args, navOptions);

                // 3. Mandamos la señal al ViewModel con un delay pequeño pero seguro
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // Quitamos el isAdded() para que la señal llegue sí o sí al ViewModel de la Activity
                    cargaViewModel.terminarProceso();
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, mensajeExito, Toast.LENGTH_SHORT).show();
                    }
                }, 800);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    limpiarCampos();
                } else {
                    validarIntentosHuella();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                validarIntentosHuella();
            }

            private void validarIntentosHuella() {
                intentosFallidosHuella++;
                if (intentosFallidosHuella >= MAX_INTENTOS_HUELLA) {
                    requireActivity().finishAffinity();
                } else {
                    Toast.makeText(getContext(), "Huella no reconocida", Toast.LENGTH_SHORT).show();
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinalShield - Identidad")
                .setSubtitle("Valida tu biometría para desbloquear")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void ocultarTeclado() {
        View view = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void manejarErrorPin() {
        intentosFallidosPin++;
        Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        for (EditText et : digitos) et.startAnimation(shake);

        if (intentosFallidosPin >= MAX_INTENTOS_PIN) {
            Navigation.findNavController(requireView()).navigate(R.id.inicio);
        } else {
            limpiarCampos();
        }
    }

    private void limpiarCampos() {
        for (EditText et : digitos) et.setText("");
        digitos[0].requestFocus();
    }

    @Override
    public void onClick(View v) {
        NavController nav = Navigation.findNavController(v);
        int id = v.getId();
        if (id == R.id.house) nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        else if (id == R.id.candadoclose) nav.navigate(R.id.archivosCifrados2);
        else if (id == R.id.carpeta) nav.navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.archivo) nav.navigate(R.id.archivosCifrados);
        else if (id == R.id.mail) nav.navigate(R.id.servivioCorreo);
        else if (id == R.id.btnperfil) nav.navigate(R.id.perfil2);
    }
}