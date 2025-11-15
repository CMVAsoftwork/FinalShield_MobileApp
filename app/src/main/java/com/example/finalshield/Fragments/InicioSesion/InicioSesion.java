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
import android.widget.Toast;

import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioSesion extends Fragment implements View.OnClickListener {
    private AuthService authService;
    private EditText inputCorreo, inputContrasena;
    private Button regre, inises, regis;

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
        regre.setOnClickListener(this);
        regis.setOnClickListener(this);
        inises.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.regresar1){
            Navigation.findNavController(v).navigate(R.id.bienvenida);
        } else if (id == R.id.btninises1) {
            String correo = inputCorreo.getText().toString();
            String pass = inputContrasena.getText().toString();

            if(correo.isEmpty() || pass.isEmpty()){
                Toast.makeText(getContext(), "Ingresa correo y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            authService.login(correo, pass, new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call,
                                       Response<LoginResponse> response) {

                    if(response.isSuccessful()){
                        Navigation.findNavController(v).navigate(R.id.datosBiometricos);
                    } else {
                        Toast.makeText(getContext(),
                                "Correo o contraseña incorrectos",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            });
            Navigation.findNavController(v).navigate(R.id.inicio);

        } else if (id == R.id.btnregis) {
            Navigation.findNavController(v).navigate(R.id.registroSesion);
        }
    }
}