package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.finalshield.Adaptadores.AdaptadorUltimos;
import com.example.finalshield.Model.Archivo;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.Service.AuthService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Inicio extends Fragment implements View.OnClickListener {

    private ListView listViewUltimos;
    private AdaptadorUltimos adaptador;
    private List<ArchivoMetadata> listaUltimos = new ArrayList<>();
    private ArchivoService archivoService;
    private AuthService authService;
    private boolean esDeepLink = false; // Bandera de seguridad

    // Elementos del Diálogo Personalizado
    private LinearLayout dialogContainer;
    private View dialogContent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Control del botón regresar físico
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalida();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // --- BLOQUE DE REDIRECCIÓN MAESTRO ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
        String tokenPendiente = prefs.getString("pending_token", null);

        if (tokenPendiente != null) {
            esDeepLink = true; // Bloqueamos procesos secundarios
            prefs.edit().remove("pending_token").apply();

            Bundle bundle = new Bundle();
            bundle.putString("security_token", tokenPendiente);

            // Salto inmediato a VerClave
            Navigation.findNavController(v).navigate(R.id.verClave, bundle);
            return;
        }
        // --- FIN DEL BLOQUE ---

        // Inicializar el servicio
        authService = new AuthService(requireContext());

        // Seguridad: Evitar capturas de pantalla
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // Referencias de UI
        dialogContainer = v.findViewById(R.id.dialogContainer);
        dialogContent = v.findViewById(R.id.dialogContentText);

        // Listeners para el diálogo de salida
        v.findViewById(R.id.sisalir).setOnClickListener(view -> requireActivity().finishAffinity());
        v.findViewById(R.id.nosalir).setOnClickListener(view -> ocultarDialogoSalida());

        // Configuración de Lista de archivos recientes
        archivoService = new ArchivoService(getContext());
        listViewUltimos = v.findViewById(R.id.listauarchivos);
        adaptador = new AdaptadorUltimos(getContext(), listaUltimos);
        listViewUltimos.setAdapter(adaptador);

        setupButtons(v);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Solo cargamos archivos si NO venimos por un Deep Link
        if (!esDeepLink) {
            fetchUltimosArchivos();
        }
    }

    private void mostrarDialogoSalida() {
        if (dialogContainer.getVisibility() == View.GONE) {
            dialogContainer.setAlpha(1f);
            dialogContainer.setVisibility(View.VISIBLE);
            Animation animIn = AnimationUtils.loadAnimation(getContext(), R.anim.dialog_fade_in);
            dialogContent.startAnimation(animIn);
        }
    }

    private void ocultarDialogoSalida() {
        Animation animOut = AnimationUtils.loadAnimation(getContext(), R.anim.dialog_fade_out);
        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                dialogContainer.setVisibility(View.GONE);
            }
        });
        dialogContent.startAnimation(animOut);
        dialogContainer.animate().alpha(0f).setDuration(250).start();
    }

    private void fetchUltimosArchivos() {
        if (archivoService == null || esDeepLink) return;

        archivoService.getAPI().getAllArchivos().enqueue(new Callback<List<Archivo>>() {
            @Override
            public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                if (!isAdded() || esDeepLink) return; // Seguridad extra

                if (response.isSuccessful() && response.body() != null) {
                    List<Archivo> todos = response.body();
                    Collections.reverse(todos);

                    listaUltimos.clear();
                    int limite = Math.min(todos.size(), 10);
                    for (int i = 0; i < limite; i++) {
                        listaUltimos.add(new ArchivoMetadata(todos.get(i)));
                    }
                    adaptador.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<Archivo>> call, Throwable t) {
                Log.e("Inicio", "Error: " + t.getMessage());
            }
        });
    }

    private void setupButtons(View v) {
        int[] ids = {R.id.btnperfil, R.id.house, R.id.archivo, R.id.candadoclose,
                R.id.carpeta, R.id.mail, R.id.candadopen, R.id.btnseleccarpeta, R.id.btnenvcorreo};
        for (int id : ids) {
            View view = v.findViewById(id);
            if (view != null) view.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.carpeta) Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.candadoclose) Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        else if (id == R.id.candadopen) Navigation.findNavController(v).navigate(R.id.filtroDescifrados);
        else if (id == R.id.btnseleccarpeta || id == R.id.archivo) Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        else if (id == R.id.btnenvcorreo || id == R.id.mail) Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        else if (id == R.id.btnperfil) Navigation.findNavController(v).navigate(R.id.perfil2);
    }
}