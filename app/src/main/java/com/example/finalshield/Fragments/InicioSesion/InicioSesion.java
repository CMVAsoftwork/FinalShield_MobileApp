package com.example.finalshield.Fragments.InicioSesion;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
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
import com.example.finalshield.ViewModel.CargaViewModel;

import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InicioSesion extends Fragment implements View.OnClickListener {

    private AuthService authService;
    private CargaViewModel cargaViewModel;
    private EditText inputCorreo, inputContrasena;
    private Button regre, inises, regis, entil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio_sesion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        authService = new AuthService(requireContext());
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        inputCorreo = v.findViewById(R.id.editcorreo);
        inputContrasena = v.findViewById(R.id.editcontraseña);
        regre = v.findViewById(R.id.regresar1);
        regis = v.findViewById(R.id.btnregis);
        inises = v.findViewById(R.id.btninises1);
        entil = v.findViewById(R.id.entil);

        regre.setOnClickListener(this);
        regis.setOnClickListener(this);
        inises.setOnClickListener(this);
        entil.setOnClickListener(this);

        verificarAccesoAutomatico(v);
    }

    // ==========================================
    // 🧬 DEEP LINK + BIOMETRIA
    // ==========================================

    private void lanzarBiometriaDirecta(View v,
                                        String desvioNotificacion,
                                        String tokenSeguro,
                                        String correo) {

        if (!isNetworkAvailable()) return;

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        NavController nav = Navigation.findNavController(v);

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {

                        super.onAuthenticationSucceeded(result);

                        int destino = R.id.inicio;

                        com.example.finalshield.MainActivity activity =
                                (com.example.finalshield.MainActivity) requireActivity();

                        if ("CIFRADOS".equals(desvioNotificacion)) {

                            destino = R.id.archivosCifrados2;
                            activity.limpiarDestinoPendiente();

                        } else if ("DESCIFRADOS".equals(desvioNotificacion)) {

                            destino = R.id.filtroDescifrados;
                            activity.limpiarDestinoPendiente();

                        } else if (tokenSeguro != null) {

                            destino = R.id.verClave;
                        }

                        irACarga(nav, destino, tokenSeguro);

                        authService.loginBiometrico(correo,
                                new Callback<LoginResponse>() {

                                    @Override
                                    public void onResponse(
                                            Call<LoginResponse> call,
                                            Response<LoginResponse> response) {

                                        if (response.isSuccessful()
                                                && response.body() != null) {

                                            authService.guardarToken(
                                                    response.body().getToken());

                                            // =====================================
                                            // GUARDAR ID DEL USUARIO
                                            // =====================================

                                            int idUsuarioRecibido =
                                                    response.body().getIdUsuario();

                                            SharedPreferences prefs =
                                                    requireContext()
                                                            .getSharedPreferences(
                                                                    "ShieldPrefs",
                                                                    Context.MODE_PRIVATE);

                                            prefs.edit()
                                                    .putInt(
                                                            "idUsuario",
                                                            idUsuarioRecibido)
                                                    .apply();

                                            Log.d(
                                                    "DEBUG_AUTH",
                                                    "ID Usuario guardado (BIO): "
                                                            + idUsuarioRecibido);

                                            // =====================================

                                            requireActivity()
                                                    .getSharedPreferences(
                                                            "deep_link",
                                                            Context.MODE_PRIVATE)
                                                    .edit()
                                                    .remove("pending_token")
                                                    .apply();

                                            cargaViewModel.terminarProceso();

                                        } else {

                                            manejarErrorNav("Sesión expirada");
                                        }
                                    }

                                    @Override
                                    public void onFailure(
                                            Call<LoginResponse> call,
                                            Throwable t) {

                                        Log.e(
                                                "RETROFIT_ERROR",
                                                "Error biométrico",
                                                t);

                                        manejarErrorNav("Error de red");
                                    }
                                });
                    }
                });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Acceso Rápido")
                        .setSubtitle("FinalShield: Verificación de identidad")
                        .setNegativeButtonText("Usar contraseña")
                        .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // ==========================================
    // 🔐 LOGIN MANUAL
    // ==========================================

    private void hacerLoginManual(View v) {

        if (!isNetworkAvailable()) {

            Toast.makeText(
                    getContext(),
                    "Sin conexión",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String correo =
                inputCorreo.getText().toString().trim();

        String pass =
                inputContrasena.getText().toString().trim();

        if (correo.isEmpty() || pass.isEmpty()) {

            Toast.makeText(
                    getContext(),
                    "Campos incompletos",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        inises.setEnabled(false);

        NavController nav =
                Navigation.findNavController(v);

        com.example.finalshield.MainActivity activity =
                (com.example.finalshield.MainActivity) requireActivity();

        String desvioNotificacion =
                activity.getDestinoPendiente();

        SharedPreferences prefs =
                requireActivity().getSharedPreferences(
                        "deep_link",
                        Context.MODE_PRIVATE);

        String pendingToken =
                prefs.getString("pending_token", null);

        int destino = R.id.inicio;

        if ("CIFRADOS".equals(desvioNotificacion)) {

            destino = R.id.archivosCifrados2;
            activity.limpiarDestinoPendiente();

        } else if ("DESCIFRADOS".equals(desvioNotificacion)) {

            destino = R.id.filtroDescifrados;
            activity.limpiarDestinoPendiente();

        } else if (pendingToken != null) {

            destino = R.id.verClave;
        }

        irACarga(nav, destino, pendingToken);

        authService.login(correo, pass,
                new Callback<LoginResponse>() {

                    @Override
                    public void onResponse(
                            Call<LoginResponse> call,
                            Response<LoginResponse> response) {

                        if (response.isSuccessful()
                                && response.body() != null) {

                            // =====================================
                            // GUARDAR ID DEL USUARIO
                            // =====================================

                            int idUsuarioRecibido =
                                    response.body().getIdUsuario();

                            SharedPreferences shieldPrefs =
                                    requireContext()
                                            .getSharedPreferences(
                                                    "ShieldPrefs",
                                                    Context.MODE_PRIVATE);

                            shieldPrefs.edit()
                                    .putInt(
                                            "idUsuario",
                                            idUsuarioRecibido)
                                    .apply();

                            Log.d(
                                    "DEBUG_AUTH",
                                    "ID Usuario guardado (MANUAL): "
                                            + idUsuarioRecibido);

                            // =====================================

                            if (pendingToken != null) {

                                prefs.edit()
                                        .remove("pending_token")
                                        .apply();
                            }

                            cargaViewModel.terminarProceso();

                        } else {

                            manejarErrorNav(
                                    "Credenciales incorrectas");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<LoginResponse> call,
                            Throwable t) {

                        Log.e(
                                "RETROFIT_ERROR",
                                "Error login manual",
                                t);

                        manejarErrorNav("Error de servidor");
                    }
                });
    }

    // ==========================================
    // 🚀 UTILIDADES
    // ==========================================

    private void irACarga(
            NavController nav,
            int destino,
            String securityToken) {

        Bundle bundle = new Bundle();

        bundle.putInt("destino_final", destino);

        if (securityToken != null) {

            Bundle argsFinales = new Bundle();

            argsFinales.putString(
                    "security_token",
                    securityToken);

            bundle.putBundle(
                    "argumentos_destino",
                    argsFinales);
        }

        nav.navigate(R.id.cargaProcesos, bundle);
    }

    private void manejarErrorNav(String msj) {

        new android.os.Handler(
                android.os.Looper.getMainLooper())
                .postDelayed(() -> {

                    if (isAdded()) {

                        cargaViewModel.resetear();

                        Navigation.findNavController(
                                        requireView())
                                .popBackStack();

                        inises.setEnabled(true);

                        Toast.makeText(
                                getContext(),
                                msj,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                }, 1000);
    }

    private void verificarAccesoAutomatico(View v) {

        SharedPreferences shieldPrefs =
                requireContext()
                        .getSharedPreferences(
                                "ShieldPrefs",
                                Context.MODE_PRIVATE);

        SharedPreferences linkPrefs =
                requireActivity()
                        .getSharedPreferences(
                                "deep_link",
                                Context.MODE_PRIVATE);

        boolean usaHuella =
                shieldPrefs.getBoolean(
                        "huella_login",
                        false);

        String pendingToken =
                linkPrefs.getString(
                        "pending_token",
                        null);

        String correoGuardado =
                authService.obtenerCorreo();

        com.example.finalshield.MainActivity activity =
                (com.example.finalshield.MainActivity) requireActivity();

        String desvioNotificacion =
                activity.getDestinoPendiente();

        if (usaHuella
                && correoGuardado != null
                && (pendingToken != null
                || desvioNotificacion != null)) {

            lanzarBiometriaDirecta(
                    v,
                    desvioNotificacion,
                    pendingToken,
                    correoGuardado);
        }
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if (id == R.id.btninises1) {

            hacerLoginManual(v);

        } else if (id == R.id.btnregis) {

            Navigation.findNavController(v)
                    .navigate(R.id.registroSesion);

        } else if (id == R.id.regresar1) {

            Navigation.findNavController(v)
                    .navigate(R.id.bienvenida);

        } else if (id == R.id.entil) {

            Navigation.findNavController(v)
                    .navigate(R.id.inicio);
        }
    }

    private boolean isNetworkAvailable() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        requireActivity()
                                .getSystemService(
                                        Context.CONNECTIVITY_SERVICE);

        NetworkInfo net =
                cm.getActiveNetworkInfo();

        return net != null && net.isConnected();
    }
}