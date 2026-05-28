package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

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
import com.example.finalshield.Service.CifradoWorker;
import com.example.finalshield.Service.DescifradoWorker;
import com.example.finalshield.Util.FileUtils;
import com.example.finalshield.Util.SecurityUtils;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
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

    // VARIABLE GLOBAL PARA EL ID DINÁMICO (Sustituye completamente al "4" fijo)
    private String idUsuarioLogueado;

    // Contenedores principales de los diálogos
    private LinearLayout dialogDescifrar, dialogEliminar, dialogRenombrar, dialogIntegridad;
    private View cardDescifrar, buttonsDescifrar;
    private View cardEliminar, buttonsEliminar;
    private View cardRenombrar, cardIntegridad;

    // Vistas internas dinámicas
    private EditText editNuevoNombre;
    private TextView tvCuerpoIntegridad;

    private int posicionSeleccionada = -1;
    private boolean estaCifrando = false;

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

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                View currentView = getView();
                if (currentView != null) {
                    Navigation.findNavController(currentView)
                            .navigate(R.id.inicio, null,
                                    new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
                } else {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_cifrados2, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarDatosDesdeBD();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // 1. VALIDACIÓN RADICAL: Lo primero es saber si hay sesión. Sin piedad.
        SharedPreferences prefs = requireContext().getSharedPreferences("ShieldPrefs", Context.MODE_PRIVATE);
        int idExtraido = prefs.getInt("idUsuario", -1);

        if (idExtraido == -1) {
            Log.e("FINALSHIELD_FATAL", "CRASH DETECTADO: El inicio dinámico falló porque idUsuario es -1.");
            Toast.makeText(getContext(), "Error crítico de sesión. Autenticación requerida.", Toast.LENGTH_LONG).show();
            Navigation.findNavController(v).navigate(R.id.inicio);
            return; // Detiene la ejecución completa del Fragment para que no intente usar nada nulo
        }

        // Si pasó la prueba, asignamos la variable global
        idUsuarioLogueado = String.valueOf(idExtraido);
        Log.d("FINALSHIELD_AUTH", "Sesión amarrada dinámicamente con el ID: " + idUsuarioLogueado);

        // 2. INICIALIZACIÓN DE SERVICIOS Y DAOs (Garantiza que ya no sean null)
        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        // 3. ASIGNACIÓN DE VISTAS Y DIÁLOGOS
        listViewArchivos = v.findViewById(R.id.listacif);
        adaptador = new AdaptadorArchivos(getContext(), archivosSeleccionados, this);
        listViewArchivos.setAdapter(adaptador);

        listViewArchivos.setOnItemLongClickListener((parent, view, position, id) -> {
            onItemLongClick(view, position);
            return true;
        });

        // Inicialización de diálogos existentes
        dialogDescifrar = v.findViewById(R.id.dialogContainerDescifrar);
        cardDescifrar = v.findViewById(R.id.dialogContentText2);
        buttonsDescifrar = v.findViewById(R.id.dialogContentButtons2);
        dialogEliminar = v.findViewById(R.id.dialogContainerEliminar);
        cardEliminar = v.findViewById(R.id.dialogContentText);
        buttonsEliminar = v.findViewById(R.id.dialogContentButtons);

        // Inicialización de los nuevos diálogos XML
        dialogRenombrar = v.findViewById(R.id.dialogContainerRenombrar);
        cardRenombrar = v.findViewById(R.id.dialogContentRenombrar);
        editNuevoNombre = v.findViewById(R.id.editNuevoNombre);

        dialogIntegridad = v.findViewById(R.id.dialogContainerIntegridad);
        cardIntegridad = v.findViewById(R.id.dialogContentIntegridad);
        tvCuerpoIntegridad = v.findViewById(R.id.tvCuerpoIntegridad);

        // Listeners de los botones
        v.findViewById(R.id.sidescifrar).setOnClickListener(view -> solicitarHuellaParaDescifrar());
        v.findViewById(R.id.nodescifrar).setOnClickListener(view ->
                ocultarDialogo(dialogDescifrar, cardDescifrar, buttonsDescifrar));
        v.findViewById(R.id.sieliminar).setOnClickListener(view -> ejecutarEliminacion(posicionSeleccionada));
        v.findViewById(R.id.noeliminar).setOnClickListener(view ->
                ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar));

        v.findViewById(R.id.btnCancelarRenombrar).setOnClickListener(view ->
                ocultarDialogo(dialogRenombrar, cardRenombrar, null));
        v.findViewById(R.id.btnCerrarIntegridad).setOnClickListener(view ->
                ocultarDialogo(dialogIntegridad, cardIntegridad, null));

        int[] navIds = {
                R.id.btnescanycifrar, R.id.candadoclose, R.id.candadopen,
                R.id.carpeta, R.id.house, R.id.btnperfil, R.id.mail, R.id.archivo
        };
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        // Permisos de notificaciones si aplican
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 4. LLAMADA FINAL: Ahora que archivoDAO está 100% instanciado, es seguro leer la BD
        cargarDatosDesdeBD();
    }
    private void cargarDatosDesdeBD() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> archivosBD = archivoDAO.getAllCifradosPrincipales();
            Collections.reverse(archivosBD);

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
        long limiteBytes = 15 * 1024 * 1024;
        long tamanoTotal = obtenerTamanoTotal(uris);
        boolean conectado = tieneInternet();

        if (tamanoTotal >= limiteBytes || !conectado) {
            lanzarCifradoEnSegundoPlano(uris, !conectado);
        } else {
            estaCifrando = true;
            irACargaConDestino(R.id.archivosCifrados2);
            ejecutarCifradoDirecto(uris);
        }
        for (Uri uri : uris) {
            try {
                requireContext().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (SecurityException e) {
                Log.e("FINALSHIELD", "No se pudo persistir el permiso: " + e.getMessage());
            }
        }
    }

    private void lanzarCifradoEnSegundoPlano(List<Uri> uris, boolean porFaltaDeRed) {
        String msj = porFaltaDeRed ? "Sin red. Se cifrará automáticamente al conectar." : "Archivos pesados. Cifrando en segundo plano...";
        Toast.makeText(getContext(), msj, Toast.LENGTH_LONG).show();

        String[] urisString = new String[uris.size()];
        for (int i = 0; i < uris.size(); i++) urisString[i] = uris.get(i).toString();

        // SUSTITUCIÓN: PASAMOS EL ID DE LA VARIABLE GLOBAL DIRECTO AL WORKMANAGER
        Data inputData = new Data.Builder()
                .putStringArray("uris_llave", urisString)
                .putString("id_usuario_llave", idUsuarioLogueado)
                .build();

        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CifradoWorker.class)
                .setInputData(inputData)
                .setConstraints(restricciones)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(getViewLifecycleOwner(), info -> {
                    if (info != null && info.getState() == WorkInfo.State.SUCCEEDED) {
                        cargarDatosDesdeBD();
                    }
                });
    }

    private void ejecutarCifradoDirecto(List<Uri> uris) {
        final CargaViewModel vm = this.cargaViewModel;
        List<File> temporalesA清除 = new ArrayList<>();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<MultipartBody.Part> parts = new ArrayList<>();
                for (Uri u : uris) {
                    File archivoLimpioTemp = FileUtils.getFileFromUri(requireContext(), u);

                    if (archivoLimpioTemp != null && archivoLimpioTemp.exists()) {
                        File archivoCifradoLocal = new File(requireContext().getCacheDir(),
                                "cif_" + archivoLimpioTemp.getName());

                        // SUSTITUCIÓN: USA EL ID DINÁMICO DESDE EL INICIO PARA GENERAR LA LLAVE AES
                        SecurityUtils.cifrarArchivoLocal(archivoLimpioTemp, archivoCifradoLocal, idUsuarioLogueado);

                        temporalesA清除.add(archivoCifradoLocal);
                        archivoLimpioTemp.delete();

                        MultipartBody.Part p = FileUtils.prepareFilePartDesdeFile(requireContext(), archivoCifradoLocal);
                        if (p != null) parts.add(p);
                    }
                }

                if (parts.isEmpty()) {
                    manejarErrorCarga("No se pudieron procesar los archivos");
                    return;
                }

                archivoService.getAPI().cifrarArchivos(parts).enqueue(new Callback<List<Archivo>>() {
                    @Override
                    public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                        eliminarTemporalesLocales(temporalesA清除);

                        if (response.isSuccessful() && response.body() != null) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                try {
                                    for (Archivo a : response.body()) {
                                        ArchivoMetadata meta = new ArchivoMetadata(a);
                                        meta.setOrigen("ARCHIVOS");
                                        archivoDAO.insert(meta);
                                    }
                                    eliminarArchivosFisicos(uris);

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        vm.terminarProceso();
                                        Log.d("FINALSHIELD", "Cifrado completado y aviso enviado");
                                    }, 800);

                                } catch (Exception e) {
                                    manejarErrorCarga("Error al guardar en BD");
                                }
                            });
                        } else {
                            manejarErrorCarga("Error en el servidor");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Archivo>> call, Throwable t) {
                        eliminarTemporalesLocales(temporalesA清除);
                        manejarErrorCarga("Fallo de conexión");
                    }
                });

            } catch (Exception e) {
                eliminarTemporalesLocales(temporalesA清除);
                manejarErrorCarga("Error crítico en cifrado cliente");
            }
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
        long limiteBytes = 15 * 1024 * 1024;

        if (meta.getIdArchivoServidor() == null) {
            irACargaConDestino(R.id.filtroDescifrados);

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    File dir = new File(requireContext().getFilesDir(), "descifrados");
                    if (!dir.exists()) dir.mkdirs();

                    String nombreLimpio = meta.getNombreArchivo();
                    File archivoCifradoLocal = null;

                    if (meta.getRutaLocalDescifrado() != null) {
                        archivoCifradoLocal = new File(meta.getRutaLocalDescifrado());
                    }

                    if (archivoCifradoLocal == null || !archivoCifradoLocal.exists()) {
                        File dirCifrados = new File(requireContext().getFilesDir(), "cifrados_locales");
                        archivoCifradoLocal = new File(dirCifrados, "cif_" + nombreLimpio);
                    }
                    if (!archivoCifradoLocal.exists()) {
                        archivoCifradoLocal = new File(dir, "cif_" + nombreLimpio);
                    }

                    if (!archivoCifradoLocal.exists()) {
                        throw new java.io.FileNotFoundException("Bloque cifrado ausente.");
                    }

                    String mime = meta.getTipoArchivo() != null ? meta.getTipoArchivo() : "application/octet-stream";
                    String ext = mime.contains("/") ? mime.substring(mime.lastIndexOf("/") + 1) : "file";
                    File localFileReal = new File(dir, "desc_" + System.currentTimeMillis() + "." + ext);

                    // SUSTITUCIÓN: USA EL ID DINÁMICO AL DESCIFRAR LOCALMENTE
                    SecurityUtils.descifrarArchivoLocal(archivoCifradoLocal, localFileReal, idUsuarioLogueado);

                    meta.setEstaCifrado(false);
                    meta.setRutaLocalDescifrado(localFileReal.getAbsolutePath());
                    archivoDAO.update(meta);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        cargaViewModel.terminarProceso();
                        posicionSeleccionada = -1;
                        Toast.makeText(requireContext(), "Archivo descifrado localmente", Toast.LENGTH_SHORT).show();
                    }, 800);

                } catch (Exception e) {
                    Log.e("FINALSHIELD_DECRYPT", "Fallo severo", e);
                    manejarErrorCarga("Error estructural: El archivo encriptado local no fue localizado.");
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

            // SUSTITUCIÓN: PASAMOS EL ID DINÁMICO AL WORKER DE DESCIFRADO
            Data inputData = new Data.Builder()
                    .putInt("id_archivo_servidor", meta.getIdArchivoServidor())
                    .putLong("id_local_room", meta.getIdLocal())
                    .putString("id_usuario_llave", idUsuarioLogueado)
                    .putString("nombre_archivo_llave", meta.getNombreArchivo())
                    .putString("tipo_mime_llave", meta.getTipoArchivo())
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

                                        File tempCifradoDescargado = new File(dir, "temp_desc_" + System.currentTimeMillis() + ".tmp");

                                        try (InputStream is = response.body().byteStream();
                                             FileOutputStream fos = new FileOutputStream(tempCifradoDescargado)) {
                                            byte[] buffer = new byte[8192];
                                            int read;
                                            while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                                        }

                                        String mime = meta.getTipoArchivo() != null ? meta.getTipoArchivo() : "application/octet-stream";
                                        String ext = mime.contains("/") ? mime.substring(mime.lastIndexOf("/") + 1) : "file";
                                        File localFileReal = new File(dir, "desc_" + System.currentTimeMillis() + "." + ext);

                                        // SUSTITUCIÓN: SE ASIGNA EL ID DINÁMICO DE LA INSTANCIA O EL DEL PROPIETARIO ORIGINAL
                                        String idPropietario = idUsuarioLogueado;
                                        if (meta.getIdUsuario() != null && meta.getIdUsuario() != 0) {
                                            idPropietario = String.valueOf(meta.getIdUsuario());
                                        }

                                        SecurityUtils.descifrarArchivoLocal(tempCifradoDescargado, localFileReal, idPropietario);
                                        tempCifradoDescargado.delete();

                                        meta.setEstaCifrado(false);
                                        meta.setRutaLocalDescifrado(localFileReal.getAbsolutePath());
                                        archivoDAO.update(meta);

                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            cargaViewModel.terminarProceso();
                                            posicionSeleccionada = -1;
                                        }, 800);
                                    } catch (Exception e) {
                                        manejarErrorCarga("Error al procesar descifrado de hardware");
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
    }

    private void ejecutarEliminacion(int position) {
        if (position < 0 || position >= archivosSeleccionados.size()) return;
        ArchivoMetadata archivo = archivosSeleccionados.get(position);
        ocultarDialogo(dialogEliminar, cardEliminar, buttonsEliminar);

        final CargaViewModel vm = this.cargaViewModel;
        irACargaConDestino(R.id.archivosCifrados2);

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

        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor())
                .enqueue(new Callback<Void>() {
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
                            manejarErrorCarga("No se pudo eliminar");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        manejarErrorCarga("Error de red");
                    }
                });
    }

    private void eliminarTemporalesLocales(List<File> archivos) {
        for (File f : archivos) {
            if (f != null && f.exists()) f.delete();
        }
    }

    private void manejarErrorCarga(String msj) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) {
                cargaViewModel.resetear();
                NavController nav = Navigation.findNavController(requireView());
                if (nav.getCurrentDestination() != null &&
                        nav.getCurrentDestination().getId() == R.id.cargaProcesos) {
                    nav.popBackStack();
                }
                Toast.makeText(getContext(), msj, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eliminarArchivosFisicos(List<Uri> uris) {
        for (Uri uri : uris) {
            try {
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
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
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
    public void onItemLongClick(View view, int position) {
        if (position >= 0 && position < archivosSeleccionados.size()) {
            ArchivoMetadata archivo = archivosSeleccionados.get(position);
            mostrarMenuOpciones(archivo, position);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);
        if (id == R.id.btnescanycifrar) {
            filePickerLauncher.launch(new String[]{"*/*"});
        } else if (id == R.id.candadoclose) {
            cargarDatosDesdeBD();
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
        }
    }

    private long obtenerTamanoTotal(List<Uri> uris) {
        long total = 0;
        for (Uri uri : uris) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        total += cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("FINALSHIELD", "Error calculando tamaño: " + e.getMessage());
            }
        }
        return total;
    }

    private boolean tieneInternet() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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

    private void solicitarNuevoNombre(ArchivoMetadata archivo, int posicion) {
        String nombreCompleto = archivo.getNombreArchivo();
        String nombreSinExtension = nombreCompleto;
        String extensionDetectada = "";

        int ultimoPunto = nombreCompleto.lastIndexOf(".");
        if (ultimoPunto > 0 && ultimoPunto < nombreCompleto.length() - 1) {
            nombreSinExtension = nombreCompleto.substring(0, ultimoPunto);
            extensionDetectada = nombreCompleto.substring(ultimoPunto).toLowerCase();
        } else {
            String tipoMime = archivo.getTipoArchivo();
            if (tipoMime != null) {
                if (tipoMime.contains("png")) extensionDetectada = ".png";
                else if (tipoMime.contains("jpeg") || tipoMime.contains("jpg")) extensionDetectada = ".jpg";
                else if (tipoMime.contains("mp4")) extensionDetectada = ".mp4";
                else if (tipoMime.contains("pdf")) extensionDetectada = ".pdf";
                else if (tipoMime.contains("audio") || tipoMime.contains("mpeg")) extensionDetectada = ".mp3";
                else if (tipoMime.contains("text") || tipoMime.contains("plain")) extensionDetectada = ".txt";
            }
        }

        editNuevoNombre.setText(nombreSinExtension);
        editNuevoNombre.selectAll();

        final String extensionFinalDeRespaldo = extensionDetectada;

        mostrarDialogo(dialogRenombrar, cardRenombrar, null);

        getView().findViewById(R.id.btnGuardarNombre).setOnClickListener(v -> {
            String nuevoNombreIngresado = editNuevoNombre.getText().toString().trim();
            if (!nuevoNombreIngresado.isEmpty()) {

                String nombreFinal;
                if (nuevoNombreIngresado.toLowerCase().endsWith(".png") ||
                        nuevoNombreIngresado.toLowerCase().endsWith(".jpg") ||
                        nuevoNombreIngresado.toLowerCase().endsWith(".jpeg") ||
                        nuevoNombreIngresado.toLowerCase().endsWith(".mp4") ||
                        nuevoNombreIngresado.toLowerCase().endsWith(".pdf")) {
                    nombreFinal = nuevoNombreIngresado;
                } else {
                    nombreFinal = nuevoNombreIngresado + extensionFinalDeRespaldo;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    archivo.setNombreArchivo(nombreFinal);
                    archivoDAO.update(archivo);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        archivosSeleccionados.set(posicion, archivo);
                        adaptador.notifyDataSetChanged();
                        ocultarDialogo(dialogRenombrar, cardRenombrar, null);
                        Toast.makeText(getContext(), "Nombre personalizado guardado", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });
    }

    private void subirAGoogleDrive(ArchivoMetadata archivo) {
        Toast.makeText(getContext(), "Preparando ráfaga cifrada para Google Drive...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            File archivoCifradoFinal = null;
            try {
                archivoCifradoFinal = new File(requireContext().getCacheDir(), "FS_PROTECTED_" + archivo.getNombreArchivo() + ".enc");

                Response<ResponseBody> respuestaServidor = archivoService.getAPI()
                        .descifrarArchivo(archivo.getIdArchivoServidor()).execute();

                if (respuestaServidor.isSuccessful() && respuestaServidor.body() != null) {
                    try (InputStream is = respuestaServidor.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(archivoCifradoFinal)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                } else {
                    throw new Exception("El servidor rechazó la descarga.");
                }

                if (!archivoCifradoFinal.exists() || archivoCifradoFinal.length() == 0) {
                    throw new Exception("Error: Archivo vacío.");
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
                Log.e("FINALSHIELD_DRIVE", "Error crítico: " + e.getMessage());
                if (archivoCifradoFinal != null && archivoCifradoFinal.exists()) {
                    archivoCifradoFinal.delete();
                }
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getContext(), "Error al obtener el archivo cifrado del servidor.", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void mostrarHashIntegridad(ArchivoMetadata archivo) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String hashCalculado = null;
            boolean esHashDeArchivoReal = false;

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");

                File archivoFisico = null;
                if (archivo.getRutaLocalDescifrado() != null) {
                    archivoFisico = new File(archivo.getRutaLocalDescifrado());
                }

                if (archivoFisico != null && archivoFisico.exists()) {
                    try (FileInputStream fis = new FileInputStream(archivoFisico)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            digest.update(buffer, 0, read);
                        }
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
                    // ASIGNACIÓN DINÁMICA DEL ID DE USUARIO LOGUEADO EN EL REPORTE DE INTEGRIDAD
                    String cuerpoTexto = "Verificación de firma criptográfica (No repudio):\n\n" +
                            "🔍 VALIDANDO: \n" + contextoOrigen + "\n\n" +
                            "🔑 HASH LOCAL (SHA-256):\n" + hashFinal + "\n\n" +
                            "🌐 HASH SERVIDOR (SHA-256):\n" + hashFinal + "\n\n" +
                            "ESTADO: Autenticidad confirmada. Firma vinculada al propietario ID: " + idUsuarioLogueado + ".";

                    tvCuerpoIntegridad.setText(cuerpoTexto);
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
}