package com.example.finalshield.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CambiarContrasena extends Fragment {
    private ImageView manoIzquierda, manoDerecha;
    private EditText etActual, etNueva;
    private Button btnActualizar, btnRegresar;
    private AuthService authService;
    private boolean brazosArriba = false;
    private ImageView robotCuerpo;
    private int BRAZOS_REPOSO_Y;
    private int BRAZOS_OJOS_Y;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cambiar_contrasena, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authService = new AuthService(requireContext());

        manoIzquierda = view.findViewById(R.id.manoIzquierda);
        manoDerecha = view.findViewById(R.id.manoDerecha);
        robotCuerpo = view.findViewById(R.id.robotCuerpo);
        etActual = view.findViewById(R.id.etPassActual);
        etNueva = view.findViewById(R.id.etPassNueva);
        btnActualizar = view.findViewById(R.id.btnActualizarC);
        btnRegresar = view.findViewById(R.id.regresarC);

        manoIzquierda.post(() -> {
            BRAZOS_REPOSO_Y = dpToPx(120);
            BRAZOS_OJOS_Y = dpToPx(-30);

            manoIzquierda.setTranslationY(BRAZOS_REPOSO_Y);
            manoDerecha.setTranslationY(BRAZOS_REPOSO_Y);
        });

        manoDerecha.post(() -> {
            manoDerecha.setPivotX(100);
            manoDerecha.setPivotY(0);
        });

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    subirBrazos();
                }
            }
        };

        etActual.addTextChangedListener(watcher);
        etNueva.addTextChangedListener(watcher);

        view.setOnClickListener(v -> {
            etActual.clearFocus();
            etNueva.clearFocus();
            bajarBrazos();
        });

        btnActualizar.setOnClickListener(v -> ejecutarCambio());
        btnRegresar.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_cambiarContrasena_to_perfil2)
        );
    }

    private void subirBrazos() {
        if (brazosArriba) return;

        brazosArriba = true;
        manoIzquierda.setVisibility(View.VISIBLE);
        manoDerecha.setVisibility(View.VISIBLE);

        manoIzquierda.animate()
                .translationY(BRAZOS_OJOS_Y)
                .setDuration(300)
                .start();

        manoDerecha.animate()
                .translationY(BRAZOS_OJOS_Y)
                .setDuration(300)
                .start();
    }

    private void bajarBrazos() {
        if (!brazosArriba) return;

        brazosArriba = false;

        manoIzquierda.animate()
                .translationY(BRAZOS_REPOSO_Y)
                .setDuration(400)
                .start();

        manoDerecha.animate()
                .translationY(BRAZOS_REPOSO_Y)
                .setDuration(400)
                .withEndAction(() -> {
                    manoIzquierda.setVisibility(View.INVISIBLE);
                    manoDerecha.setVisibility(View.INVISIBLE);
                })
                .start();
    }

    private void brazosRectos() {
        brazosArriba = false;

        manoIzquierda.setVisibility(View.VISIBLE);
        manoDerecha.setVisibility(View.VISIBLE);

        manoIzquierda.animate()
                .translationX(dpToPx(-55))
                .translationY(0)
                .setDuration(300)
                .start();

        manoDerecha.animate()
                .translationX(dpToPx(50))
                .translationY(0)
                .setDuration(300)
                .start();
    }

    private void animacionVictoria() {
        subirBrazos();

        manoIzquierda.animate()
                .translationX(dpToPx(-50))
                .setDuration(200)
                .withEndAction(this::soltarAplauso)
                .start();

        manoDerecha.animate()
                .translationX(dpToPx(40))
                .setDuration(200)
                .start();
    }

    private void soltarAplauso() {
        manoIzquierda.animate()
                .translationY(dpToPx(50))
                .translationX(dpToPx(-70))
                .rotation(-185)
                .setDuration(600)
                .start();

        manoDerecha.animate()
                .translationY(dpToPx(157))
                .translationX(155)
                .rotation(195)
                .setDuration(600)
                .start();
    }

    private void animacionSalida(Runnable onFinish) {
        bajarBrazos();

        robotCuerpo.animate()
                .translationY(dpToPx(320))
                .alpha(0f)
                .setDuration(700)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(onFinish)
                .start();
    }

    private void ejecutarCambio() {
        String actual = etActual.getText().toString().trim();
        String nueva = etNueva.getText().toString().trim();

        if (actual.isEmpty() || nueva.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nueva.length() < 8) {
            etNueva.setError("La contraseña debe tener al menos 8 caracteres");
            return;
        }

        if (actual.equals(nueva)) {
            Toast.makeText(getContext(), "La nueva contraseña no puede ser igual a la actual", Toast.LENGTH_SHORT).show();
            return;
        }

        authService.cambiarContraseña(actual, nueva, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (isAdded()) {
                        animacionVictoria();
                        Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show();

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (!isAdded()) return;

                            animacionSalida(() -> {
                                if (!isAdded()) return;

                                authService.cerrarSesion();

                                NavHostFragment.findNavController(CambiarContrasena.this)
                                        .navigate(
                                                R.id.inicioSesion,
                                                null,
                                                new NavOptions.Builder()
                                                        .setPopUpTo(R.id.naviegador, true)
                                                        .build()
                                        );
                            });

                        }, 1500);
                    }
                } else {
                    Toast.makeText(getContext(), "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp * 3;
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}