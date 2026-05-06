package com.example.finalshield.Fragments.Correo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finalshield.DTO.Correo.CorreoRequest;
import com.example.finalshield.R;
import com.example.finalshield.Service.CorreoService;
import com.example.finalshield.Util.FileUtils;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServivioCorreo extends Fragment implements View.OnClickListener {

    private EditText etDestinatario, etAsunto, etMensaje;
    private Button btnAdjuntar, btnEnviar, btnCerrarLista;
    private FrameLayout btnVerArchivos;
    private TextView badgeContador;
    private LinearLayout dialogListaAdjuntos;
    private ListView lvArchivosSeleccionados;

    private CorreoService correoService;
    private CargaViewModel cargaViewModel;
    private final List<Uri> adjuntosUri = new ArrayList<>();
    private ActivityResultLauncher<String[]> filePickerLauncher;

    private int intentosHuella = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // IMPORTANTE: requireActivity() para compartir el estado con el Fragment de Carga
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
            if (uris != null) {
                for (Uri uri : uris) {
                    if (!adjuntosUri.contains(uri)) adjuntosUri.add(uri);
                }
                actualizarBadge();
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (dialogListaAdjuntos.getVisibility() == View.VISIBLE) {
                    dialogListaAdjuntos.setVisibility(View.GONE);
                } else {
                    if (isAdded()) {
                        Navigation.findNavController(requireView()).navigate(R.id.inicio, null,
                                new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_servivio_correo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        correoService = new CorreoService(requireContext());
        etDestinatario = v.findViewById(R.id.paracontend);
        etAsunto = v.findViewById(R.id.asuntocontend);
        etMensaje = v.findViewById(R.id.mensajecontent);
        btnAdjuntar = v.findViewById(R.id.adjuntarArchivo);
        btnEnviar = v.findViewById(R.id.enviarCorreo);
        btnVerArchivos = v.findViewById(R.id.btnVerArchivos);
        badgeContador = v.findViewById(R.id.badgeContador);
        dialogListaAdjuntos = v.findViewById(R.id.dialogListaAdjuntos);
        lvArchivosSeleccionados = v.findViewById(R.id.lvArchivosSeleccionados);
        btnCerrarLista = v.findViewById(R.id.btnCerrarLista);

        btnAdjuntar.setOnClickListener(v1 -> filePickerLauncher.launch(new String[]{"*/*"}));
        btnEnviar.setOnClickListener(view -> validarHuella());
        btnVerArchivos.setOnClickListener(v1 -> mostrarListaManual());
        btnCerrarLista.setOnClickListener(v1 -> dialogListaAdjuntos.setVisibility(View.GONE));

        int[] navIds = {R.id.btnperfil, R.id.house, R.id.archivo, R.id.candadoclose, R.id.carpeta, R.id.mail, R.id.candadopen};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }
        actualizarBadge();
    }

    private void actualizarBadge() {
        if (badgeContador == null) return;
        badgeContador.setText(String.valueOf(adjuntosUri.size()));
        btnVerArchivos.setVisibility(View.VISIBLE);
        btnVerArchivos.setAlpha(adjuntosUri.isEmpty() ? 0.4f : 1.0f);
    }

    private void mostrarListaManual() {
        if (adjuntosUri.isEmpty()) {
            Toast.makeText(getContext(), "No hay archivos seleccionados", Toast.LENGTH_SHORT).show();
            return;
        }
        dialogListaAdjuntos.setVisibility(View.VISIBLE);
        ArrayAdapter<Uri> adapter = new ArrayAdapter<Uri>(requireContext(), R.layout.item_archivo_adjunto, adjuntosUri) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_archivo_adjunto, parent, false);
                TextView tvNombre = convertView.findViewById(R.id.tvNombreArchivoItem);
                ImageView btnEliminar = convertView.findViewById(R.id.imgEliminar);
                Uri uriActual = getItem(position);
                if (uriActual != null) tvNombre.setText(FileUtils.getFileName(getContext(), uriActual));
                btnEliminar.setOnClickListener(v -> {
                    adjuntosUri.remove(position);
                    notifyDataSetChanged();
                    actualizarBadge();
                    if (adjuntosUri.isEmpty()) dialogListaAdjuntos.setVisibility(View.GONE);
                });
                return convertView;
            }
        };
        lvArchivosSeleccionados.setAdapter(adapter);
    }

    private void validarHuella() {
        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "Sin conexión a Internet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etDestinatario.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Ingresa un destinatario", Toast.LENGTH_SHORT).show();
            return;
        }
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                intentosHuella = 0;
                enviarCorreoReal();
            }
            @Override
            public void onAuthenticationError(int err, @NonNull CharSequence str) {
                if (err != BiometricPrompt.ERROR_USER_CANCELED) verificarBloqueo();
            }
            @Override
            public void onAuthenticationFailed() { verificarBloqueo(); }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinalShield - Confirmar Envío")
                .setSubtitle("Verifica tu identidad")
                .setNegativeButtonText("Cancelar")
                .build();
        prompt.authenticate(info);
    }

    private void verificarBloqueo() {
        intentosHuella++;
        if (intentosHuella >= 5) requireActivity().finishAffinity();
        else Toast.makeText(getContext(), "Huella no reconocida " + intentosHuella + "/5", Toast.LENGTH_SHORT).show();
    }

    private void enviarCorreoReal() {
        if (!isAdded()) return;

        // 1. Preparamos el bundle para indicarle a la carga a donde ir al final
        Bundle bundleCarga = new Bundle();
        bundleCarga.putInt("destino_final", R.id.servivioCorreo);

        // 2. Navegamos a la pantalla de carga para que el usuario espere con la animación
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, bundleCarga);

        // 3. Iniciamos el proceso de envío
        procesarPeticionEnvio();
    }

    private void procesarPeticionEnvio() {
        String asunto = etAsunto.getText().toString().trim();
        String mensaje = etMensaje.getText().toString().trim();
        String destino = etDestinatario.getText().toString().trim();

        CorreoRequest req = new CorreoRequest(asunto, mensaje, destino);
        RequestBody jsonBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"),
                correoService.getGson().toJson(req));

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri u : adjuntosUri) {
            MultipartBody.Part p = FileUtils.prepareFilePart(requireContext(), u);
            if (p != null) parts.add(p);
        }

        correoService.getAPI().enviarCorreo(jsonBody, parts).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Avisamos al ViewModel para que la carga haga su animación de salida
                    cargaViewModel.terminarProceso();

                    mainHandler.postDelayed(() -> {
                        if (isAdded()) {
                            limpiarTodo();
                            Toast.makeText(requireContext(), "Enviado con éxito", Toast.LENGTH_SHORT).show();
                        }
                    }, 500);

                } else {
                    mainHandler.post(() -> {
                        cargaViewModel.resetear();
                        // Si falla, regresamos al fragmento de correo
                        Navigation.findNavController(requireView()).popBackStack();
                        Toast.makeText(requireContext(), "Error en el servidor", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                mainHandler.post(() -> {
                    cargaViewModel.resetear();
                    Navigation.findNavController(requireView()).popBackStack();
                    Toast.makeText(requireContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void limpiarTodo() {
        etAsunto.setText(""); etMensaje.setText(""); etDestinatario.setText("");
        adjuntosUri.clear(); actualizarBadge();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);
        if (id == R.id.house) nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        else if (id == R.id.carpeta) nav.navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.candadoclose) nav.navigate(R.id.archivosCifrados2);
        else if (id == R.id.candadopen) nav.navigate(R.id.filtroDescifrados);
        else if (id == R.id.archivo) nav.navigate(R.id.archivosCifrados);
        else if (id == R.id.btnperfil) nav.navigate(R.id.perfil2);
    }
}