package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.Util.SecurityUtils; // Importamos tu nueva utilidad
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CifradoEscaneo2 extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private final List<ArchivoMetadata> listaArchivos = new ArrayList<>();
    private ListView listView;
    private AdaptadorArchivos adaptador;
    private ArchivoDAO archivoDAO;
    private ArchivoService archivoService;
    private CargaViewModel cargaViewModel;

    private LinearLayout dialogDescifrar, dialogEliminar;
    private View cardDescifrar, buttonsDescifrar;
    private View cardEliminar, buttonsEliminar;

    private int posicionSeleccionada = -1;
    private int intentosHuella = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.inicio, null,
                        new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cifrado_escaneo2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        listView = v.findViewById(R.id.listaescan);
        dialogDescifrar = v.findViewById(R.id.dialogContainer);
        cardDescifrar = v.findViewById(R.id.dialogContentText);
        buttonsDescifrar = v.findViewById(R.id.dialogContentButtons);

        dialogEliminar = v.findViewById(R.id.dialogContainer2);
        cardEliminar = v.findViewById(R.id.dialogContentText2);
        buttonsEliminar = v.findViewById(R.id.dialogContentButtons2);

        adaptador = new AdaptadorArchivos(getContext(), listaArchivos, this);
        listView.setAdapter(adaptador);

        v.findViewById(R.id.sieliminar).setOnClickListener(view -> solicitarHuellaParaDescifrar());
        v.findViewById(R.id.noeliminar).setOnClickListener(view -> ocultarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar));

        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> ejecutarEliminacion(posicionSeleccionada));
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar));

        int[] navIds = {R.id.scan, R.id.btnperfil, R.id.house, R.id.archivo, R.id.candadoclose, R.id.carpeta, R.id.mail, R.id.candadopen};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        cargarDatosDesdeBD();
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
                .setSubtitle("Autentícate para descifrar el escaneo")
                .setNegativeButtonText("Cancelar")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void ejecutarDescifrado(int position) {
        if (position < 0 || position >= listaArchivos.size()) return;
        ArchivoMetadata meta = listaArchivos.get(position);
        final CargaViewModel vm = this.cargaViewModel;

        Bundle args = new Bundle();
        args.putInt("destino_final", R.id.filtroDescifrados);
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);

        archivoService.getAPI().descifrarArchivo(meta.getIdArchivoServidor())
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    File dir = new File(requireContext().getFilesDir(), "descifrados");
                                    if (!dir.exists()) dir.mkdirs();
                                    File localFile = new File(dir, "escaneo_" + System.currentTimeMillis() + ".pdf");

                                    try (InputStream is = response.body().byteStream();
                                         FileOutputStream fos = new FileOutputStream(localFile)) {
                                        byte[] buffer = new byte[8192];
                                        int read;
                                        while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                                    }

                                    meta.setEstaCifrado(false);
                                    meta.setRutaLocalDescifrado(localFile.getAbsolutePath());
                                    meta.setOrigen("ESCANEO");
                                    archivoDAO.update(meta);

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        vm.terminarProceso();
                                    }, 800);

                                } catch (Exception e) {
                                    manejarErrorCarga("Error al guardar escaneo");
                                }
                            });
                        } else {
                            manejarErrorCarga("Error en el servidor");
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                        manejarErrorCarga("Error de conexión");
                    }
                });
    }

    private void ejecutarEliminacion(int position) {
        if (position < 0 || position >= listaArchivos.size()) return;

        final ArchivoMetadata archivo = listaArchivos.get(position);
        final CargaViewModel vm = this.cargaViewModel;
        final Context appCtx = requireContext().getApplicationContext();

        ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar);

        Bundle args = new Bundle();
        args.putInt("destino_final", R.id.cifradoEscaneo2);
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);

        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        // --- USO DE LA UTILIDAD SECURITYUTILS ---
                        if (archivo.getRutaLocalDescifrado() != null) {
                            SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
                        }

                        archivoDAO.delete(archivo);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            vm.terminarProceso();
                            Toast.makeText(appCtx, "Escaneo destruido permanentemente", Toast.LENGTH_SHORT).show();
                        }, 1000);
                    });
                } else {
                    manejarErrorCarga("No se pudo eliminar del servidor");
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                manejarErrorCarga("Fallo de red");
            }
        });
    }

    // EL MÉTODO destruirArchivoFisico FUE ELIMINADO DE AQUÍ PORQUE YA ESTÁ EN SECURITYUTILS

    private void manejarErrorCarga(String msj) {
        new Handler(Looper.getMainLooper()).post(() -> {
            cargaViewModel.resetear();
            if (isAdded()) {
                Navigation.findNavController(requireView()).popBackStack();
                Toast.makeText(requireContext(), msj, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarDatosDesdeBD() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> archivosBD = archivoDAO.getAllCifradosEscaneo();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    listaArchivos.clear();
                    listaArchivos.addAll(archivosBD);
                    adaptador.notifyDataSetChanged();
                }
            });
        });
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

    @Override public void onDescifrarClick(int position) { posicionSeleccionada = position; mostrarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar); }
    @Override public void onBorrarClick(int position) { posicionSeleccionada = position; mostrarDialogo(dialogEliminar, cardEliminar, buttonsEliminar); }
    @Override public void onItemClick(int pos) { Toast.makeText(getContext(), "Archivo cifrado.", Toast.LENGTH_SHORT).show(); }
    @Override public void onCambiarEstadoClick(int pos) {}

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);
        if (id == R.id.house) nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        else if (id == R.id.candadoclose) nav.navigate(R.id.archivosCifrados2);
        else if (id == R.id.candadopen) nav.navigate(R.id.filtroDescifrados);
        else if (id == R.id.carpeta) nav.navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.btnperfil) nav.navigate(R.id.perfil2);
        else if (id == R.id.mail) nav.navigate(R.id.servivioCorreo);
        else if (id == R.id.archivo) nav.navigate(R.id.archivosCifrados);
        else if (id == R.id.scan) nav.navigate(R.id.opcionCifrado2);
    }
}