package com.example.finalshield.Fragments.InicioSesion;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroSesion extends Fragment implements View.OnClickListener {
    private EditText inputNombre, inputPaterno, inputMaterno, inputCorreo, inputContrasena, inputConfContrasena;
    private RadioButton radioBiometrico;
    private AuthService authService;
    private Button regre, iniises, regis;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        authService = new AuthService(requireContext());

        inputNombre = v.findViewById(R.id.nombre);
        inputPaterno = v.findViewById(R.id.paterno);
        inputMaterno = v.findViewById(R.id.materno);
        inputCorreo = v.findViewById(R.id.correo);
        inputContrasena = v.findViewById(R.id.contraseña);
        inputConfContrasena = v.findViewById(R.id.confcontra);

        radioBiometrico = v.findViewById(R.id.btlRadio);
        regre = v.findViewById(R.id.regresar2);
        iniises = v.findViewById(R.id.inisesi);
        regis = v.findViewById(R.id.regis);

        regis.setOnClickListener(this);
        regre.setOnClickListener(this);
        iniises.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.regis){
            String nombre = inputNombre.getText().toString().trim();
            String paterno = inputPaterno.getText().toString().trim();
            String materno = inputMaterno.getText().toString().trim();
            String correo = inputCorreo.getText().toString().trim();
            String contrasena = inputContrasena.getText().toString();
            String confContrasena = inputConfContrasena.getText().toString();

            if(nombre.isEmpty() || paterno.isEmpty() || materno.isEmpty() ||
                    correo.isEmpty() || contrasena.isEmpty() || confContrasena.isEmpty()) {
                Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!contrasena.equals(confContrasena)) {
                Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            authService.registro(nombre + " " + paterno + " " + materno, correo, contrasena, new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if(response.isSuccessful() && response.body() != null) {
                        Toast.makeText(getContext(), "Registro exitoso", Toast.LENGTH_SHORT).show();

                        authService.guardarCorreo(correo);
                        authService.guardarToken(response.body().getToken());

                        if(radioBiometrico.isChecked()) {
                            authService.habilitarBiometrico(correo, true, new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> resp) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("correo_biometrico", correo);

                                    Navigation.findNavController(v).navigate(R.id.datosBiometricos, bundle);
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(getContext(), "Error al habilitar biometría", Toast.LENGTH_SHORT).show();
                                    Navigation.findNavController(v).navigate(R.id.inicio);
                                }
                            });
                        } else {
                            Navigation.findNavController(v).navigate(R.id.inicio);
                        }

                    } else {
                        Toast.makeText(getContext(), "Error al registrar: " +
                                (response.message() != null ? response.message() : "Revise los datos"), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (id == R.id.inisesi) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        } else if (id == R.id.regresar2) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        }
    }
}