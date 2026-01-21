package com.example.finalshield.Fragments.InicioSesion;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Bienvenida extends Fragment implements View.OnClickListener {
    ImageView ivrotation;
    ObjectAnimator animatorrotation;
    long animationduration = 400;
    int iniHu;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bienvenida, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ivrotation = v.findViewById(R.id.brazo);
        animatorrotation = ObjectAnimator.ofFloat(ivrotation, "rotation", -15f, -40f);
        animatorrotation.setDuration(animationduration);
        animatorrotation.setRepeatCount(ValueAnimator.INFINITE);
        animatorrotation.setRepeatMode(ValueAnimator.REVERSE);
        animatorrotation.start();
        Button comenzar;
        comenzar = v.findViewById(R.id.btncomenzar);
        comenzar.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        AuthService authService = new AuthService(requireContext());
        String correoGuardado = authService.obtenerCorreo();

        if (correoGuardado != null) {
            // Consultamos al servidor si este correo tiene el biométrico activo
            authService.isBiometricoActivo(correoGuardado, new Callback<Boolean>() {
                @Override
                public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                    Bundle bundle = new Bundle();
                    if (response.isSuccessful() && Boolean.TRUE.equals(response.body())) {
                        // SI tiene huella: Ir a Biométricos pasando por carga
                        bundle.putInt("destino_final", R.id.datosBiometricos);
                    } else {
                        // NO tiene huella: Ir a Inicio Sesión pasando por carga
                        bundle.putInt("destino_final", R.id.inicioSesion);
                    }
                    Navigation.findNavController(v).navigate(R.id.cargaProcesos, bundle);
                }

                @Override
                public void onFailure(Call<Boolean> call, Throwable t) {
                    // Si falla el servidor, por seguridad vamos a Inicio Sesión manual
                    Bundle bundle = new Bundle();
                    bundle.putInt("destino_final", R.id.inicioSesion);
                    Navigation.findNavController(v).navigate(R.id.cargaProcesos, bundle);
                }
            });
        } else {
            // Usuario nuevo: Ir a Inicio Sesión
            Bundle bundle = new Bundle();
            bundle.putInt("destino_final", R.id.inicioSesion);
            Navigation.findNavController(v).navigate(R.id.cargaProcesos, bundle);
        }
    }
}