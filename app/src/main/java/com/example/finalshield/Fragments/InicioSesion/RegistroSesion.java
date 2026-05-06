package com.example.finalshield.Fragments.InicioSesion;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.finalshield.DTO.Usuario.LoginResponse;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.ViewModel.CargaViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegistroSesion extends Fragment implements View.OnClickListener {

    private EditText inputNombre, inputPaterno, inputMaterno, inputCorreo,
            inputContrasena, inputConfContrasena, inputPinReal, inputPinFalso;
    private CheckBox checkBiometrico;
    private AuthService authService;
    private CargaViewModel cargaViewModel; // ViewModel para controlar la animación
    private Button regre, iniises, regis;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registro_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        authService = new AuthService(requireContext());
        // Inicializar el ViewModel compartido
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        inputNombre = v.findViewById(R.id.nombre);
        inputPaterno = v.findViewById(R.id.paterno);
        inputMaterno = v.findViewById(R.id.materno);
        inputCorreo = v.findViewById(R.id.correo);
        inputContrasena = v.findViewById(R.id.nuevaContrasena);
        inputConfContrasena = v.findViewById(R.id.confcontra);
        inputPinReal = v.findViewById(R.id.pinReal);
        inputPinFalso = v.findViewById(R.id.pinSeguro);
        checkBiometrico = v.findViewById(R.id.btlHuella);

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
        if (id == R.id.regis) {
            ejecutarRegistro(v);
        } else if (id == R.id.inisesi || id == R.id.regresar2) {
            Navigation.findNavController(v).navigate(R.id.inicioSesion);
        }
    }

    private void ejecutarRegistro(View v) {
        String nombre = inputNombre.getText().toString().trim();
        String paterno = inputPaterno.getText().toString().trim();
        String materno = inputMaterno.getText().toString().trim();
        String correo = inputCorreo.getText().toString().trim();
        String contrasena = inputContrasena.getText().toString();
        String confContrasena = inputConfContrasena.getText().toString();
        String pinR = inputPinReal.getText().toString().trim();
        String pinF = inputPinFalso.getText().toString().trim();

        // 1. Validaciones previas (Sin carga aún)
        if (nombre.isEmpty() || paterno.isEmpty() || materno.isEmpty() ||
                correo.isEmpty() || contrasena.isEmpty() || pinR.isEmpty() || pinF.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!contrasena.equals(confContrasena)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pinR.length() != 6 || pinF.length() != 6) {
            Toast.makeText(getContext(), "Los PINs deben ser de 6 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- INICIO DE PROCESO DINÁMICO ---
        regis.setEnabled(false);
        NavController nav = Navigation.findNavController(v);

        // Determinamos el destino ANTES de ir a carga
        int destinoFinal = checkBiometrico.isChecked() ? R.id.datosBiometricos : R.id.inicio;

        // 2. Navegamos a la pantalla de carga
        irACarga(nav, destinoFinal, correo);

        // 3. Llamada al servicio mientras se muestra la animación
        String nombreCompleto = nombre + " " + paterno + " " + materno;
        authService.registro(nombreCompleto, correo, contrasena, pinR, pinF, new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    authService.guardarCorreo(correo);
                    authService.guardarToken(response.body().getToken());

                    if (checkBiometrico.isChecked()) {
                        // Si usa huella, habilitamos en el server y luego soltamos la carga
                        habilitarBiometriaYFinalizar(correo);
                    } else {
                        // Registro normal, soltamos la carga directamente
                        cargaViewModel.terminarProceso();
                    }
                } else {
                    manejarErrorRegistro("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                manejarErrorRegistro("Error de conexión");
            }
        });
    }

    private void habilitarBiometriaYFinalizar(String correo) {
        authService.habilitarBiometrico(correo, true, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> resp) {
                cargaViewModel.terminarProceso();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Aunque falle la habilitación, dejamos que entre a Inicio
                cargaViewModel.terminarProceso();
            }
        });
    }

    private void irACarga(NavController nav, int destino, String correo) {
        Bundle bundleCarga = new Bundle();
        bundleCarga.putInt("destino_final", destino);

        // Si el destino es datosBiometricos, necesita el correo como argumento
        if (destino == R.id.datosBiometricos) {
            Bundle argsFinales = new Bundle();
            argsFinales.putString("correo_biometrico", correo);
            bundleCarga.putBundle("argumentos_destino", argsFinales);
        }

        nav.navigate(R.id.cargaProcesos, bundleCarga);
    }

    private void manejarErrorRegistro(String msj) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                cargaViewModel.resetear();
                Navigation.findNavController(requireView()).popBackStack(); // Regresamos al formulario
                regis.setEnabled(true);
                Toast.makeText(getContext(), msj, Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }
}