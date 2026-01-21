package com.example.finalshield.Fragments.InicioSesion;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.finalshield.API.AuthAPI;
import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioSesion extends Fragment implements View.OnClickListener {
    private AuthService authService;
    private EditText inputCorreo, inputContrasena;
    private Button regre, inises, regis, entil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        authService = new AuthService(requireContext());

        inputCorreo = v.findViewById(R.id.editcorreo);
        inputContrasena = v.findViewById(R.id.editcontraseña);
        regre = v.findViewById(R.id.regresar1);
        regis = v.findViewById(R.id.btnregis);
        inises = v.findViewById(R.id.btninises1);
        entil = v.findViewById(R.id.btnnxd);

        regre.setOnClickListener(this);
        regis.setOnClickListener(this);
        inises.setOnClickListener(this);
        entil.setOnClickListener(this);

        // --- SE ELIMINÓ EL AUTO-SALTO A BIOMÉTRICOS AQUÍ ---
        // Para que el usuario pueda escribir su correo/pass sin que lo saquen de la pantalla.
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnnxd) Navigation.findNavController(v).navigate(R.id.inicio);

        if (id == R.id.regresar1) {
            Navigation.findNavController(v).navigate(R.id.bienvenida);
        } else if (id == R.id.btninises1) {
            hacerLoginManual(v);
        } else if (id == R.id.btnregis) {
            Navigation.findNavController(v).navigate(R.id.registroSesion);
        }
    }

    private void hacerLoginManual(View v) {
        String correo = inputCorreo.getText().toString().trim();
        String pass = inputContrasena.getText().toString().trim();

        if (correo.isEmpty() || pass.isEmpty()) {
            Toast.makeText(getContext(), "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        authService.login(correo, pass, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    authService.guardarCorreo(correo);
                    authService.guardarToken(response.body().getToken());

                    // SI YA SE LOGUEÓ MANUALMENTE, MANDAR DIRECTO AL INICIO
                    // No tiene sentido mandarlo a biométricos si ya puso la contraseña
                    handlePostLoginNavigation(v, R.id.inicio);
                } else {
                    Toast.makeText(getContext(), "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePostLoginNavigation(View view, int defaultDestination) {
        if (!isAdded()) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        String pendingToken = prefs.getString("pending_token", null);

        if (pendingToken != null) {
            prefs.edit().remove("pending_token").apply();
            Bundle args = new Bundle();
            args.putString("security_token", pendingToken);
            try {
                Navigation.findNavController(view).navigate(R.id.action_inicioSesion_to_verClavePostLogin, args);
            } catch (Exception e) {
                Navigation.findNavController(view).navigate(R.id.verClave, args);
            }
        } else {
            // Aseguramos que la navegación ocurra en el hilo principal
            requireActivity().runOnUiThread(() -> {
                try {
                    Navigation.findNavController(view).navigate(defaultDestination);
                } catch (Exception e) {
                    // Fallback por si la vista se descolgó
                    NavHostFragment.findNavController(InicioSesion.this).navigate(defaultDestination);
                }
            });
        }
    }
}