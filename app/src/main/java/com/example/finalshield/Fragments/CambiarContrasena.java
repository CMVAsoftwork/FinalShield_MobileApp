package com.example.finalshield.Fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;

import java.util.concurrent.Executor;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.lifecycle.ViewModelProvider;
import com.example.finalshield.ViewModel.CargaViewModel;

public class CambiarContrasena extends Fragment {

    private ImageView manoIzquierda, manoDerecha;
    private EditText etActual, etNueva;
    private Button btnActualizar, btnRegresar;
    private AuthService authService;
    private CargaViewModel cargaViewModel; // El control de la carga

    private boolean mostrarActual = false;
    private boolean mostrarNueva = false;
    private int BRAZOS_REPOSO_Y;
    private int BRAZOS_OJOS_Y;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializar el ViewModel compartido
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavHostFragment.findNavController(CambiarContrasena.this).popBackStack();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cambiar_contrasena, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new AuthService(requireContext());
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        manoIzquierda = view.findViewById(R.id.manoIzquierda);
        manoDerecha   = view.findViewById(R.id.manoDerecha);
        etActual      = view.findViewById(R.id.etPassActual);
        etNueva       = view.findViewById(R.id.etPassNueva);
        btnActualizar = view.findViewById(R.id.btnActualizarC);
        btnRegresar   = view.findViewById(R.id.regresarC);

        // Lógica de brazos post-layout
        manoIzquierda.post(() -> {
            BRAZOS_REPOSO_Y = dpToPx(120);
            BRAZOS_OJOS_Y   = dpToPx(-30);
            manoIzquierda.setTranslationY(BRAZOS_REPOSO_Y);
            manoDerecha.setTranslationY(BRAZOS_REPOSO_Y);
            subirBrazoIzquierdo();
            subirBrazoDerecho();
        });

        configurarOjito(etActual, true);
        configurarOjito(etNueva, false);

        btnActualizar.setOnClickListener(v -> validarHuellaYCambiar());
        btnRegresar.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void validarHuellaYCambiar() {
        String actual = etActual.getText().toString();
        String nueva  = etNueva.getText().toString();

        if (actual.isEmpty() || nueva.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                // 1. Ir a carga PRIMERO
                irACargaYProcesar(actual, nueva);
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                Toast.makeText(getContext(), "Autenticación necesaria", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(getContext(), "Huella no reconocida", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Cambio de Seguridad")
                .setSubtitle("Confirma para actualizar")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(info);
    }

    private void irACargaYProcesar(String actual, String nueva) {
        NavController nav = Navigation.findNavController(requireView());

        // Configuramos el destino: queremos que después de la carga vaya a InicioSesion
        Bundle bundleCarga = new Bundle();
        bundleCarga.putInt("destino_final", R.id.inicioSesion);

        nav.navigate(R.id.cargaProcesos, bundleCarga);

        // 2. Ejecutar petición de red
        authService.cambiarContraseña(actual, nueva, new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Cierras sesión localmente
                    authService.cerrarSesion();
                    // Le decimos a CargaProcesos que haga su gracia y navegue
                    cargaViewModel.terminarProceso();
                    Toast.makeText(getContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                } else {
                    manejarErrorServidor("Datos incorrectos");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                manejarErrorServidor("Error de conexión");
            }
        });
    }

    private void manejarErrorServidor(String msj) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                cargaViewModel.resetear();
                Navigation.findNavController(requireView()).popBackStack(); // Regresa al formulario
                Toast.makeText(getContext(), msj, Toast.LENGTH_LONG).show();
            }
        }, 1000);
    }

    // --- (El resto de tus métodos de animación y dpToPx se mantienen exactamente igual) ---
    private void togglePassword(EditText editText, boolean mostrar, boolean esIzquierdo) {
        if (mostrar) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ojoabierto, 0);
            if (esIzquierdo) bajarBrazoIzquierdo(); else bajarBrazoDerecho();
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ojocerrado, 0);
            if (esIzquierdo) subirBrazoIzquierdo(); else subirBrazoDerecho();
        }
        editText.setSelection(editText.getText().length());
    }

    private void subirBrazoIzquierdo() {
        manoIzquierda.setVisibility(View.VISIBLE);
        manoIzquierda.animate().translationY(BRAZOS_OJOS_Y).setDuration(300).start();
    }

    private void subirBrazoDerecho() {
        manoDerecha.setVisibility(View.VISIBLE);
        manoDerecha.animate().translationY(BRAZOS_OJOS_Y).setDuration(300).start();
    }

    private void bajarBrazoIzquierdo() {
        manoIzquierda.animate().translationY(BRAZOS_REPOSO_Y).setDuration(400)
                .withEndAction(() -> {
                    if (manoIzquierda.getTranslationY() == BRAZOS_REPOSO_Y) manoIzquierda.setVisibility(View.INVISIBLE);
                }).start();
    }

    private void bajarBrazoDerecho() {
        manoDerecha.animate().translationY(BRAZOS_REPOSO_Y).setDuration(400)
                .withEndAction(() -> {
                    if (manoDerecha.getTranslationY() == BRAZOS_REPOSO_Y) manoDerecha.setVisibility(View.INVISIBLE);
                }).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configurarOjito(EditText editText, boolean esActual) {
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (editText.getCompoundDrawables()[2] != null) {
                    int iconWidth = editText.getCompoundDrawables()[2].getBounds().width();
                    if (event.getRawX() >= (editText.getRight() - iconWidth - 50)) {
                        if (esActual) {
                            mostrarActual = !mostrarActual;
                            togglePassword(editText, mostrarActual, true);
                        } else {
                            mostrarNueva = !mostrarNueva;
                            togglePassword(editText, mostrarNueva, false);
                        }
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp * 3;
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}