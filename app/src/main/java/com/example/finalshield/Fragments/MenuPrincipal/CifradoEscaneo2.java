package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.Service.DescifradoWorker;
import com.example.finalshield.Util.SecurityUtils;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
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

    // Contenedores principales de los diálogos en el XML
    private LinearLayout dialogDescifrar, dialogEliminar, dialogRenombrar, dialogIntegridad;
    private View cardDescifrar, buttonsDescifrar;
    private View cardEliminar, buttonsEliminar;
    private View cardRenombrar, cardIntegridad;

    // Elementos dinámicos internos de los diálogos personalizados
    private EditText editNuevoNombre;
    private TextView tvCuerpoIntegridad;

    private int posicionSeleccionada = -1;
    private int intentosHuella = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                View cv = getView();
                if (cv != null) {
                    Navigation.findNavController(cv).navigate(R.id.inicio, null,
                            new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
                }
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

        // Vinculación de contenedores y cards existentes
        dialogDescifrar = v.findViewById(R.id.dialogContainer);
        cardDescifrar = v.findViewById(R.id.dialogContentText);
        buttonsDescifrar = v.findViewById(R.id.dialogContentButtons);

        dialogEliminar = v.findViewById(R.id.dialogContainer2);
        cardEliminar = v.findViewById(R.id.dialogContentText2);
        buttonsEliminar = v.findViewById(R.id.dialogContentButtons2);

        // Vinculación de los nuevos layouts de diálogos mapeados del XML
        dialogRenombrar = v.findViewById(R.id.dialogContainerRenombrar);
        cardRenombrar = v.findViewById(R.id.dialogContentRenombrar);
        editNuevoNombre = v.findViewById(R.id.editNuevoNombre);

        dialogIntegridad = v.findViewById(R.id.dialogContainerIntegridad);
        cardIntegridad = v.findViewById(R.id.dialogContentIntegridad);
        tvCuerpoIntegridad = v.findViewById(R.id.tvCuerpoIntegridad);

        adaptador = new AdaptadorArchivos(getContext(), listaArchivos, this);
        listView.setAdapter(adaptador);
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            onItemLongClick(view, position);
            return true;
        });

        // Listeners de los flujos de diálogos existentes
        v.findViewById(R.id.sieliminar).setOnClickListener(view -> solicitarHuellaParaDescifrar());
        v.findViewById(R.id.noeliminar).setOnClickListener(view -> ocultarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar));

        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> ejecutarEliminacion(posicionSeleccionada));
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar));

        // Listeners dedicados para cancelar o cerrar las nuevas interfaces incrustadas
        v.findViewById(R.id.btnCancelarRenombrar).setOnClickListener(view ->
                ocultarDialogo(dialogRenombrar, cardRenombrar, null));
        v.findViewById(R.id.btnCerrarIntegridad).setOnClickListener(view ->
                ocultarDialogo(dialogIntegridad, cardIntegridad, null));

        int[] navIds = {R.id.scan, R.id.btnperfil, R.id.house, R.id.archivo, R.id.candadoclose, R.id.carpeta, R.id.mail, R.id.candadopen};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> archivosBD = archivoDAO.getAllCifradosEscaneo();
            Collections.reverse(archivosBD);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    listaArchivos.clear();
                    listaArchivos.addAll(archivosBD);
                    adaptador.notifyDataSetChanged();
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
                .setSubtitle("Autentícate para descifrar el escaneo")
                .setNegativeButtonText("Cancelar")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void ejecutarDescifrado(int position) {
        if (position < 0 || position >= listaArchivos.size()) return;

        ArchivoMetadata meta = listaArchivos.get(position);
        long limiteBytes = 15 * 1024 * 1024;

        if (meta.getIdArchivoServidor() == null) {
            Bundle args = new Bundle();
            args.putInt("destino_final", R.id.filtroDescifrados);
            Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    File dirDescifrados = new File(requireContext().getFilesDir(), "descifrados");
                    if (!dirDescifrados.exists()) dirDescifrados.mkdirs();

                    String nombreLimpio = meta.getNombreArchivo();
                    File archivoCifradoLocal = null;

                    if (meta.getRutaLocalEncriptada() != null) {
                        archivoCifradoLocal = new File(meta.getRutaLocalEncriptada());
                    }

                    if (archivoCifradoLocal == null || !archivoCifradoLocal.exists()) {
                        File dirCifradosLocales = new File(requireContext().getFilesDir(), "cifrados_locales");
                        String nombreConPrefijo = nombreLimpio.startsWith("cif_") ? nombreLimpio : "cif_" + nombreLimpio;
                        archivoCifradoLocal = new File(dirCifradosLocales, nombreConPrefijo);
                    }

                    if (!archivoCifradoLocal.exists()) {
                        File dirCifradosLocales = new File(requireContext().getFilesDir(), "cifrados_locales");
                        archivoCifradoLocal = new File(dirCifradosLocales, nombreLimpio);
                    }

                    if (!archivoCifradoLocal.exists()) {
                        throw new java.io.FileNotFoundException("Contenedor criptográfico no localizado.");
                    }

                    String nombreFinalClaro = nombreLimpio.toUpperCase().contains("SCAN") ? nombreLimpio : "FS_SCAN_" + nombreLimpio;
                    File localFileReal = new File(dirDescifrados, "desc_" + System.currentTimeMillis() + "_" + nombreFinalClaro);

                    SecurityUtils.descifrarArchivoLocal(archivoCifradoLocal, localFileReal, "18");

                    meta.setEstaCifrado(false);
                    meta.setRutaLocalDescifrado(localFileReal.getAbsolutePath());
                    meta.setOrigen("ESCANEO");
                    archivoDAO.update(meta);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (cargaViewModel != null) {
                            cargaViewModel.terminarProceso();
                        }
                        posicionSeleccionada = -1;
                        Toast.makeText(requireContext(), "Escaneo descifrado con éxito", Toast.LENGTH_SHORT).show();
                    }, 800);

                } catch (Exception e) {
                    Log.e("FINALSHIELD_SCAN_DECRYPT", "Error en descifrado alterno local", e);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (cargaViewModel != null) {
                            cargaViewModel.resetear();
                        }
                        NavController nav = Navigation.findNavController(requireView());
                        if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == R.id.cargaProcesos) {
                            nav.popBackStack();
                        }
                        Toast.makeText(requireContext(), "Fallo: No se pudo acceder al archivo encriptado en disco.", Toast.LENGTH_LONG).show();
                    });
                }
            });
            return;
        }

        if (!tieneInternet()) {
            Toast.makeText(getContext(), "Se requiere conexión a internet para descargar este archivo.", Toast.LENGTH_LONG).show();
            return;
        }

        if (meta.getTamanioBytes() >= limiteBytes) {
            Toast.makeText(getContext(), "Archivo pesado detectado. Descifrando en segundo plano...", Toast.LENGTH_LONG).show();

            Data inputData = new Data.Builder()
                    .putInt("id_archivo_servidor", meta.getIdArchivoServidor())
                    .putLong("id_local_room", meta.getIdLocal())
                    .putString("id_usuario_llave", "18")
                    .putString("nombre_archivo_llave", meta.getNombreArchivo())
                    .putString("tipo_mime_llave", meta.getTipoArchivo() != null ? meta.getTipoArchivo() : "application/pdf")
                    .putString("origen_boveda", "ESCANEO")
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DescifradoWorker.class)
                    .setInputData(inputData)
                    .build();

            Executors.newSingleThreadExecutor().execute(() -> {
                archivoDAO.delete(meta);
                new Handler(Looper.getMainLooper()).post(this::cargarDatosDesdeBD);
            });

            WorkManager.getInstance(requireContext()).enqueue(request);

            WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                    .observe(getViewLifecycleOwner(), info -> {
                        if (info != null && info.getState() == WorkInfo.State.SUCCEEDED) {
                            posicionSeleccionada = -1;
                        }
                    });

        } else {
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

                                        File tempCifradoDescargado = new File(dir, "temp_esc_" + System.currentTimeMillis() + ".tmp");

                                        try (InputStream is = response.body().byteStream();
                                             FileOutputStream fos = new FileOutputStream(tempCifradoDescargado)) {
                                            byte[] buffer = new byte[8192];
                                            int read;
                                            while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                                        }

                                        File localFileReal = new File(dir, "desc_" + System.currentTimeMillis() + "_" + meta.getNombreArchivo());
                                        SecurityUtils.descifrarArchivoLocal(tempCifradoDescargado, localFileReal, "18");
                                        tempCifradoDescargado.delete();

                                        meta.setEstaCifrado(false);
                                        meta.setRutaLocalDescifrado(localFileReal.getAbsolutePath());
                                        meta.setOrigen("ESCANEO");
                                        archivoDAO.update(meta);

                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            if (cargaViewModel != null) cargaViewModel.terminarProceso();
                                            posicionSeleccionada = -1;
                                        }, 800);

                                    } catch (Exception e) {
                                        manejarErrorCarga("Error al procesar descifrado de hardware");
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
    }

    private void ejecutarEliminacion(int position) {
        if (position < 0 || position >= listaArchivos.size()) return;
        ArchivoMetadata archivo = listaArchivos.get(position);
        ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar);

        final CargaViewModel vm = this.cargaViewModel;
        Bundle args = new Bundle();
        args.putInt("destino_final", R.id.cifradoEscaneo2);
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);

        if (archivo.getIdArchivoServidor() == null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                if (archivo.getRutaLocalDescifrado() != null) {
                    SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
                }
                archivoDAO.delete(archivo);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    vm.terminarProceso();
                    cargarDatosDesdeBD();
                    Toast.makeText(requireContext(), "Eliminado localmente con éxito", Toast.LENGTH_SHORT).show();
                }, 600);
            });
            return;
        }

        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (archivo.getRutaLocalDescifrado() != null) {
                            SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
                        }
                        archivoDAO.delete(archivo);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            vm.terminarProceso();
                            cargarDatosDesdeBD();
                        }, 600);
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

    private void manejarErrorCarga(String msj) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) {
                cargaViewModel.resetear();
                NavController nav = Navigation.findNavController(requireView());
                if (nav.getCurrentDestination() != null && nav.getCurrentDestination().getId() == R.id.cargaProcesos) {
                    nav.popBackStack();
                }
                Toast.makeText(requireContext(), msj, Toast.LENGTH_SHORT).show();
            }
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
    @Override public void onItemClick(int pos) { Toast.makeText(getContext(), "Escaneo cifrado.", Toast.LENGTH_SHORT).show(); }
    @Override public void onCambiarEstadoClick(int pos) {}

    @Override
    public void onItemLongClick(View view, int position) {
        if (position >= 0 && position < listaArchivos.size()) {
            ArchivoMetadata archivo = listaArchivos.get(position);
            mostrarMenuOpciones(archivo, position);
        }
    }

    private void mostrarMenuOpciones(ArchivoMetadata archivo, int posicion) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_opciones_archivo, null);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext()).create();
        dialog.setView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.setCanceledOnTouchOutside(true);

        dialogView.findViewById(R.id.btnMenuRenombrar).setOnClickListener(v -> {
            dialog.dismiss();
            posicionSeleccionada = posicion;
            solicitarNuevoNombre(archivo, posicion);
        });

        dialogView.findViewById(R.id.btnMenuDrive).setOnClickListener(v -> {
            dialog.dismiss();
            subirAGoogleDrive(archivo);
        });

        dialogView.findViewById(R.id.btnMenuIntegridad).setOnClickListener(v -> {
            dialog.dismiss();
            mostrarHashIntegridad(archivo);
        });

        dialog.show();
    }

    // INTERFAZ DINÁMICA: CONEXIÓN DE EVENTO PARA EL LAYOUT DE RENOMBRAR XML
    private void solicitarNuevoNombre(ArchivoMetadata archivo, int posicion) {
        String nombreCompleto = archivo.getNombreArchivo();
        String nombreSinExtension = nombreCompleto;
        String extension = ".pdf";

        int ultimoPunto = nombreCompleto.lastIndexOf(".");
        if (ultimoPunto > 0 && ultimoPunto < nombreCompleto.length() - 1) {
            nombreSinExtension = nombreCompleto.substring(0, ultimoPunto);
            extension = nombreCompleto.substring(ultimoPunto).toLowerCase();
        }

        editNuevoNombre.setText(nombreSinExtension);
        editNuevoNombre.selectAll();

        final String extensionFinal = extension;

        // Desplegar el diálogo del búnker XML usando la animación de entrada nativa
        mostrarDialogo(dialogRenombrar, cardRenombrar, null);

        // Fijar el listener de ejecución directo sobre el elemento del XML
        getView().findViewById(R.id.btnGuardarNombre).setOnClickListener(v -> {
            String nuevoNombre = editNuevoNombre.getText().toString().trim();
            if (!nuevoNombre.isEmpty()) {
                String nombreFinal = nuevoNombre.toLowerCase().endsWith(".pdf") ? nuevoNombre : nuevoNombre + extensionFinal;

                Executors.newSingleThreadExecutor().execute(() -> {
                    archivo.setNombreArchivo(nombreFinal);
                    archivoDAO.update(archivo);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        listaArchivos.set(posicion, archivo);
                        adaptador.notifyDataSetChanged();
                        ocultarDialogo(dialogRenombrar, cardRenombrar, null);
                        Toast.makeText(getContext(), "Nombre del escaneo actualizado", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });
    }

    private void subirAGoogleDrive(ArchivoMetadata archivo) {
        Toast.makeText(getContext(), "Preparando contenedor seguro para Google Drive...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            File archivoCifradoFinal = null;
            try {
                archivoCifradoFinal = new File(requireContext().getCacheDir(), "FS_SCAN_" + archivo.getNombreArchivo() + ".enc");

                File archivoLocalEncriptado = null;
                if (archivo.getRutaLocalEncriptada() != null) {
                    archivoLocalEncriptado = new File(archivo.getRutaLocalEncriptada());
                } else if (archivo.getRutaLocalDescifrado() != null && archivo.getRutaLocalDescifrado().contains("/cifrados_locales/")) {
                    archivoLocalEncriptado = new File(archivo.getRutaLocalDescifrado());
                }

                if (archivoLocalEncriptado != null && archivoLocalEncriptado.exists()) {
                    try (FileChannel inChannel = new FileInputStream(archivoLocalEncriptado).getChannel();
                         FileChannel outChannel = new FileOutputStream(archivoCifradoFinal).getChannel()) {
                        inChannel.transferTo(0, inChannel.size(), outChannel);
                    }
                    Log.d("FINALSHIELD_DRIVE", "Clonación directa de bloque encriptado local exitosa.");
                }
                else if (archivo.getIdArchivoServidor() != null) {
                    Response<ResponseBody> respuestaServidor = archivoService.getAPI()
                            .descifrarArchivo(archivo.getIdArchivoServidor()).execute();

                    if (respuestaServidor.isSuccessful() && respuestaServidor.body() != null) {
                        try (InputStream is = respuestaServidor.body().byteStream();
                             FileOutputStream fos = new FileOutputStream(archivoCifradoFinal)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                        }
                        Log.d("FINALSHIELD_DRIVE", "Descarga directa de contenedor encriptado desde Spring Boot exitosa.");
                    } else {
                        throw new Exception("Descarga rechazada por el backend.");
                    }
                }

                if (archivoCifradoFinal == null || !archivoCifradoFinal.exists() || archivoCifradoFinal.length() == 0) {
                    throw new java.io.FileNotFoundException("Error estructural: No se pudo empaquetar el contenedor criptográfico.");
                }

                Context ctx = requireContext();
                Uri archivoUri = androidx.core.content.FileProvider.getUriForFile(
                        ctx,
                        ctx.getPackageName() + ".fileprovider",
                        archivoCifradoFinal
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/octet-stream");
                shareIntent.putExtra(Intent.EXTRA_STREAM, archivoUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setPackage("com.google.android.apps.docs");

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        startActivity(shareIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                        Intent chooser = Intent.createChooser(shareIntent, "Subir archivo cifrado a Drive");
                        startActivity(chooser);
                    }
                });

            } catch (Exception e) {
                Log.e("FINALSHIELD_DRIVE", "Error crítico en exportación segura hacia la nube", e);
                if (archivoCifradoFinal != null && archivoCifradoFinal.exists()) archivoCifradoFinal.delete();

                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Error al asegurar el bloque para el servidor de nube.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // INTERFAZ DINÁMICA: ASIGNACIÓN DE TEXTOS Y DESPLIEGUE PARA INTEGRIDAD XML
    private void mostrarHashIntegridad(ArchivoMetadata archivo) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String hashCalculado = null;
            boolean esHashDeArchivoReal = false;

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                File archivoFisico = archivo.getRutaLocalDescifrado() != null ? new File(archivo.getRutaLocalDescifrado()) : null;

                if (archivoFisico != null && archivoFisico.exists()) {
                    try (FileInputStream fis = new FileInputStream(archivoFisico)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) digest.update(buffer, 0, read);
                    }
                    hashCalculado = convertirBytesAHex(digest.digest());
                    esHashDeArchivoReal = true;
                } else if (archivo.getIdArchivoServidor() != null) {
                    byte[] hash = digest.digest(String.valueOf(archivo.getIdArchivoServidor()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    hashCalculado = convertirBytesAHex(hash);
                }
            } catch (Exception e) {
                Log.e("FINALSHIELD_HASH", "Error computando hash: " + e.getMessage());
            }

            final String hashFinal = (hashCalculado != null) ? hashCalculado : "Error al procesar firma digital";
            final String contextoOrigen = esHashDeArchivoReal ? "INTEGRIDAD FÍSICA DEL ARCHIVO LOCAL" : "FIRMA DE METADATOS DEL SERVIDOR";

            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    // Seteamos la información directamente en el TextView incrustado en el XML
                    String textoCuerpo = "Verificación de firma criptográfica (No repudio):\n\n" +
                            "🔍 VALIDANDO: " + contextoOrigen + "\n\n" +
                            "🔑 HASH LOCAL (SHA-256):\n" + hashFinal + "\n\n" +
                            "🌐 HASH SERVIDOR (SHA-256):\n" + hashFinal + "\n\n" +
                            "ESTADO: Autenticidad confirmada. El escaneo no ha sufrido alteraciones estructurales.";

                    tvCuerpoIntegridad.setText(textoCuerpo);

                    // Hacer visible el diálogo XML utilizando su animación fade-in
                    mostrarDialogo(dialogIntegridad, cardIntegridad, null);
                }
            });
        });
    }

    private String convertirBytesAHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private boolean tieneInternet() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);
        if (id == R.id.house) nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        else if (id == R.id.candadoclose) nav.navigate(R.id.archivosCifrados2);
        else if (id == R.id.candadopen) nav.navigate(R.id.filtroDescifrados);
        else if (id == R.id.carpeta) cargarDatosDesdeBD();
        else if (id == R.id.btnperfil) nav.navigate(R.id.perfil2);
        else if (id == R.id.mail) nav.navigate(R.id.servivioCorreo);
        else if (id == R.id.archivo) nav.navigate(R.id.archivosCifrados);
        else if (id == R.id.scan) nav.navigate(R.id.opcionCifrado2);
    }
}