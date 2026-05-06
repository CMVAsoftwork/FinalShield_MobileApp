package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.Archivo;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.Util.FileUtils;
import com.example.finalshield.Util.SecurityUtils; // Importamos la utilidad
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArchivosCifrados2 extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private final List<ArchivoMetadata> archivosSeleccionados = new ArrayList<>();
    private ListView listViewArchivos;
    private AdaptadorArchivos adaptador;
    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;
    private CargaViewModel cargaViewModel;
    private int intentosHuella = 0;
    private LinearLayout dialogDescifrar, dialogEliminar;
    private View cardDescifrar, buttonsDescifrar;
    private View cardEliminar, buttonsEliminar;
    private int posicionSeleccionada = -1;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    enviarAlServidor(uris);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView())
                        .navigate(R.id.inicio, null,
                                new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_cifrados2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); // Protección extra

        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        listViewArchivos = v.findViewById(R.id.listacif);
        adaptador = new AdaptadorArchivos(getContext(), archivosSeleccionados, this);
        listViewArchivos.setAdapter(adaptador);

        dialogDescifrar = v.findViewById(R.id.dialogContainerDescifrar);
        cardDescifrar = v.findViewById(R.id.dialogContentText2);
        buttonsDescifrar = v.findViewById(R.id.dialogContentButtons2);
        dialogEliminar = v.findViewById(R.id.dialogContainerEliminar);
        cardEliminar = v.findViewById(R.id.dialogContentText);
        buttonsEliminar = v.findViewById(R.id.dialogContentButtons);

        v.findViewById(R.id.sidescifrar).setOnClickListener(view -> solicitarHuellaParaDescifrar());
        v.findViewById(R.id.nodescifrar).setOnClickListener(view ->
                ocultarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar));
        v.findViewById(R.id.sieliminar).setOnClickListener(view -> ejecutarEliminacion(posicionSeleccionada));
        v.findViewById(R.id.noeliminar).setOnClickListener(view ->
                ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar));

        int[] navIds = {
                R.id.btnescanycifrar, R.id.candadoclose, R.id.candadopen,
                R.id.carpeta, R.id.house, R.id.btnperfil, R.id.mail, R.id.archivo
        };
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }
        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> archivosBD = archivoDAO.getAllCifradosPrincipales();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    archivosSeleccionados.clear();
                    archivosSeleccionados.addAll(archivosBD);
                    adaptador.notifyDataSetChanged();
                }
            });
        });
    }

    private void irACargaConDestino(int destinoId) {
        Bundle args = new Bundle();
        args.putInt("destino_final", destinoId);
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);
    }

    private void enviarAlServidor(List<Uri> uris) {
        irACargaConDestino(R.id.archivosCifrados2);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<MultipartBody.Part> parts = new ArrayList<>();
            for (Uri u : uris) {
                MultipartBody.Part p = FileUtils.prepareFilePartArchivo(requireContext(), u);
                if (p != null) parts.add(p);
            }
            archivoService.getAPI().cifrarArchivos(parts).enqueue(new Callback<List<Archivo>>() {
                @Override
                public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            for (Archivo a : response.body()) {
                                ArchivoMetadata meta = new ArchivoMetadata(a);
                                meta.setOrigen("ARCHIVOS");
                                archivoDAO.insert(meta);
                            }
                            // Aquí eliminamos los archivos originales del teléfono
                            eliminarArchivosFisicos(uris);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(getContext(), "Cifrado exitoso y archivos originales removidos", Toast.LENGTH_SHORT).show();
                                cargaViewModel.terminarProceso();
                            });
                        });
                    } else {
                        manejarErrorCarga("Error al cifrar");
                    }
                }
                @Override
                public void onFailure(Call<List<Archivo>> call, Throwable t) {
                    manejarErrorCarga("Fallo de red");
                }
            });
        });
    }

    private void solicitarHuellaParaDescifrar() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                intentosHuella = 0;
                ocultarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar);
                ejecutarDescifrado(posicionSeleccionada);
            }
            @Override
            public void onAuthenticationError(int errCode, @NonNull CharSequence errString) {
                if (errCode != BiometricPrompt.ERROR_USER_CANCELED) validarIntentos();
            }
            @Override
            public void onAuthenticationFailed() {
                validarIntentos();
            }
            private void validarIntentos() {
                intentosHuella++;
                if (intentosHuella >= 5) requireActivity().finishAffinity();
                else Toast.makeText(getContext(), "Error (" + intentosHuella + "/5)", Toast.LENGTH_SHORT).show();
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinalShield - Seguridad")
                .setSubtitle("Confirma tu identidad")
                .setNegativeButtonText("Cancelar")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void ejecutarDescifrado(int position) {
        if (position < 0 || position >= archivosSeleccionados.size()) return;
        ArchivoMetadata meta = archivosSeleccionados.get(position);
        irACargaConDestino(R.id.filtroDescifrados);

        archivoService.getAPI().descifrarArchivo(meta.getIdArchivoServidor())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    File dir = new File(requireContext().getFilesDir(), "descifrados");
                                    if (!dir.exists()) dir.mkdirs();
                                    String mime = meta.getTipoArchivo() != null ? meta.getTipoArchivo() : "application/octet-stream";
                                    String ext = mime.contains("/") ? mime.substring(mime.lastIndexOf("/") + 1) : "file";
                                    File localFile = new File(dir, "desc_" + System.currentTimeMillis() + "." + ext);

                                    try (InputStream is = response.body().byteStream();
                                         FileOutputStream fos = new FileOutputStream(localFile)) {
                                        byte[] buffer = new byte[8192];
                                        int read;
                                        while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                                    }
                                    meta.setEstaCifrado(false);
                                    meta.setRutaLocalDescifrado(localFile.getAbsolutePath());
                                    archivoDAO.update(meta);

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> cargaViewModel.terminarProceso(), 800);
                                } catch (Exception e) {
                                    manejarErrorCarga("Error al guardar archivo");
                                }
                            });
                        } else {
                            manejarErrorCarga("Error en servidor");
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        manejarErrorCarga("Error de red");
                    }
                });
    }

    private void ejecutarEliminacion(int position) {
        if (position < 0 || position >= archivosSeleccionados.size()) return;
        ArchivoMetadata archivo = archivosSeleccionados.get(position);
        ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar);

        irACargaConDestino(R.id.archivosCifrados2);

        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                // --- BORRADO SEGURO DE LA CACHÉ LOCAL SI EXISTIERA ---
                                if (archivo.getRutaLocalDescifrado() != null) {
                                    SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
                                }

                                archivoDAO.delete(archivo);

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    Toast.makeText(getContext(), "Destruido de la bóveda", Toast.LENGTH_SHORT).show();
                                    cargaViewModel.terminarProceso();
                                }, 1000);
                            });
                        } else {
                            manejarErrorCarga("No se pudo eliminar");
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        manejarErrorCarga("Error de red");
                    }
                });
    }

    private void manejarErrorCarga(String msj) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                cargaViewModel.resetear();
                Navigation.findNavController(requireView()).popBackStack();
                Toast.makeText(getContext(), msj, Toast.LENGTH_SHORT).show();
            }
        }, 800);
    }

    private void eliminarArchivosFisicos(List<Uri> uris) {
        for (Uri uri : uris) {
            try {
                // Para archivos externos, usamos DocumentFile para pedir al sistema que los borre
                DocumentFile doc = DocumentFile.fromSingleUri(requireContext(), uri);
                if (doc != null && doc.exists()) {
                    doc.delete();
                }
            } catch (Exception e) {
                Log.e("FINALSHIELD", "No se pudo borrar el archivo original: " + uri.toString());
            }
        }
    }

    private void mostrarDialogo(LinearLayout container, View card, View buttons) {
        container.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.dialog_fade_in);
        if (card != null) card.startAnimation(anim);
        if (buttons != null) buttons.startAnimation(anim);
    }

    private void ocultarDialogo(LinearLayout container, View card, View buttons) {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.dialog_fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                container.setVisibility(View.GONE);
                posicionSeleccionada = -1;
            }
        });
        if (card != null) card.startAnimation(anim);
        if (buttons != null) buttons.startAnimation(anim);
    }

    @Override
    public void onDescifrarClick(int position) {
        posicionSeleccionada = position;
        mostrarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar);
    }

    @Override
    public void onBorrarClick(int position) {
        posicionSeleccionada = position;
        mostrarDialogo(dialogEliminar, cardEliminar, buttonsEliminar);
    }

    @Override
    public void onItemClick(int pos) {
        Toast.makeText(getContext(), "Archivo cifrado.", Toast.LENGTH_SHORT).show();
    }

    @Override public void onCambiarEstadoClick(int pos) {}

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);
        if (id == R.id.btnescanycifrar) {
            filePickerLauncher.launch(new String[]{"*/*"});
        } else if (id == R.id.candadopen) {
            nav.navigate(R.id.filtroDescifrados);
        } else if (id == R.id.carpeta) {
            nav.navigate(R.id.cifradoEscaneo2);
        } else if (id == R.id.house) {
            nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        } else if (id == R.id.btnperfil) {
            nav.navigate(R.id.perfil2);
        } else if (id == R.id.mail) {
            nav.navigate(R.id.servivioCorreo);
        } else if (id == R.id.archivo) {
            nav.navigate(R.id.archivosCifrados);
        } else if (id == R.id.candadoclose) {
            cargarDatosDesdeBD();
        }
    }
}