package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
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
import androidx.core.content.FileProvider;
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
import com.example.finalshield.Util.SecurityUtils;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ArchivosDSeguros extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private ListView listView;
    private AdaptadorArchivos adaptador;
    private final List<ArchivoMetadata> listaMetadata = new ArrayList<>();
    private ArchivoDAO archivoDAO;
    private CargaViewModel cargaViewModel;

    private String idUsuarioLogueado;

    private LinearLayout dialogContainerCifrar, dialogContainerEliminar, dialogContainerIntegridad;
    private View cardIntegridad;
    private TextView tvCuerpoIntegridad;

    private int posicionSeleccionada = -1;

    private final ActivityResultLauncher<Intent> drivePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        procesarImportacionFalsaDesdeDrive(uri);
                    }
                }
            });

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
        return inflater.inflate(R.layout.fragment_archivos_d_seguros, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("ShieldPrefs", Context.MODE_PRIVATE);
        int idExtraido = prefs.getInt("idUsuario", -1);

        if (idExtraido == -1) {
            Log.e("FINALSHIELD_HONEYPOT", "Bóveda señuelo bloqueada: No hay usuario en SharedPreferences.");
            Toast.makeText(getContext(), "Error crítico de sesión. Autenticación requerida.", Toast.LENGTH_LONG).show();
            Navigation.findNavController(v).navigate(R.id.inicio);
            return;
        }
        idUsuarioLogueado = String.valueOf(idExtraido);

        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();
        listView = v.findViewById(R.id.listadesc);

        dialogContainerCifrar = v.findViewById(R.id.dialogContainer);
        dialogContainerEliminar = v.findViewById(R.id.dialogContainer2);
        dialogContainerIntegridad = v.findViewById(R.id.dialogContainerIntegridad);
        cardIntegridad = v.findViewById(R.id.dialogContentIntegridad);
        tvCuerpoIntegridad = v.findViewById(R.id.tvCuerpoIntegridad);

        adaptador = new AdaptadorArchivos(getContext(), listaMetadata, this);
        listView.setAdapter(adaptador);

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            onItemLongClick(view, position);
            return true;
        });

        // ASIGNACIÓN DIRECTA CORREGIDA PARA CERRAR DIÁLOGOS SIN RIESGO DE ANIMACIÓN NULL
        v.findViewById(R.id.sicifrar).setOnClickListener(view -> ejecutarBloqueoReCifrado());
        v.findViewById(R.id.nocifrar).setOnClickListener(view -> {
            dialogContainerCifrar.setVisibility(View.GONE);
            posicionSeleccionada = -1;
        });

        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> ejecutarAccionFalsaEliminar());
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> {
            dialogContainerEliminar.setVisibility(View.GONE);
            posicionSeleccionada = -1;
        });

        if (v.findViewById(R.id.btnCerrarIntegridad) != null) {
            v.findViewById(R.id.btnCerrarIntegridad).setOnClickListener(view ->
                    ocultarDialogo(dialogContainerIntegridad, cardIntegridad, null));
        }

        View btnDrive = v.findViewById(R.id.btnImportarDrive);
        if (btnDrive != null) btnDrive.setOnClickListener(this);

        int[] navIds = {R.id.house, R.id.candadoclose, R.id.carpeta, R.id.archivo, R.id.mail, R.id.btnperfil, R.id.candadopen};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        clonarEstructuraReal();
    }

    private void clonarEstructuraReal() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> datosReales = archivoDAO.getAllDescifrados();
            Collections.reverse(datosReales);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    listaMetadata.clear();
                    listaMetadata.addAll(datosReales);
                    adaptador.notifyDataSetChanged();
                }
            });
        });
    }

    @Override
    public void onItemClick(int position) {
        if (position < 0 || position >= listaMetadata.size()) return;
        ArchivoMetadata archivo = listaMetadata.get(position);

        Executors.newSingleThreadExecutor().execute(() -> {
            File clonFalsoMimetizado = generarClonFalsoEspejo(archivo);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (clonFalsoMimetizado == null || !clonFalsoMimetizado.exists()) {
                    Toast.makeText(getContext(), "Error crítico de descifrado local (Firma inválida)", Toast.LENGTH_SHORT).show();
                    return;
                }
                procederAAbrirArchivoFalso(archivo, clonFalsoMimetizado);
            });
        });
    }

    private void procederAAbrirArchivoFalso(ArchivoMetadata archivo, File file) {
        try {
            String nombreOriginal = archivo.getNombreArchivo();
            String extension = "";
            int ultimoPunto = nombreOriginal.lastIndexOf(".");
            if (ultimoPunto > 0 && ultimoPunto < nombreOriginal.length() - 1) {
                extension = nombreOriginal.substring(ultimoPunto + 1).toLowerCase();
            }

            if (new Random().nextInt(4) == 0) {
                Toast.makeText(getContext(), "Error de red (503): El backend de FinalShield no respondió a tiempo. Reintentando...", Toast.LENGTH_LONG).show();
                return;
            }

            if (!extension.equals("jpg") && !extension.equals("png") && !extension.equals("jpeg") &&
                    !extension.equals("mp3") && !extension.equals("aac") && !extension.equals("wav") &&
                    !extension.equals("mp4") && !extension.equals("3gp")) {

                Toast.makeText(getContext(), "Fallo al procesar el documento: El servidor devolvió un flujo de bytes corrupto o incompleto. Error al mostrar el contenido.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Descifrando bloque de hardware... Advertencia: Posible desfase de bits en bloque local.", Toast.LENGTH_SHORT).show();
            }

            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType == null) {
                mimeType = (archivo.getTipoArchivo() != null && !archivo.getTipoArchivo().equals("application/octet-stream"))
                        ? archivo.getTipoArchivo() : "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Abrir con:"));

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error crítico 0xEF43: El descifrador del sandbox no pudo inicializar el renderizado del buffer.", Toast.LENGTH_SHORT).show();
        }
    }

    private File generarClonFalsoEspejo(ArchivoMetadata archivoReal) {
        try {
            String nombreOriginal = archivoReal.getNombreArchivo();
            long pesoDestino = archivoReal.getTamanioBytes();
            if (pesoDestino <= 0) pesoDestino = 1024 * 30;

            String extension = "";
            int ultimoPunto = nombreOriginal.lastIndexOf(".");
            if (ultimoPunto > 0 && ultimoPunto < nombreOriginal.length() - 1) {
                extension = nombreOriginal.substring(ultimoPunto + 1).toLowerCase();
            }

            File archivoFalso = new File(requireContext().getCacheDir(), "decoy_vault_" + nombreOriginal);
            if (archivoFalso.exists() && archivoFalso.length() == pesoDestino) return archivoFalso;

            long semillaUnica = nombreOriginal.hashCode() + pesoDestino;
            Random randomizador = new Random(semillaUnica);

            try (FileOutputStream fos = new FileOutputStream(archivoFalso)) {

                if (extension.equals("jpg") || extension.equals("png") || extension.equals("jpeg")) {
                    int dim = 250 + randomizador.nextInt(100);
                    Bitmap estatica = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);

                    for (int y = 0; y < dim; y++) {
                        for (int x = 0; x < dim; x++) {
                            int r = randomizador.nextInt(256);
                            estatica.setPixel(x, y, Color.rgb(r, r, r));
                        }
                    }

                    int calidadFalsa = 60 + randomizador.nextInt(25);
                    estatica.compress(Bitmap.CompressFormat.JPEG, calidadFalsa, fos);
                    estatica.recycle();

                } else if (extension.equals("mp3") || extension.equals("aac") || extension.equals("wav")) {
                    fos.write("RIFF".getBytes());
                    fos.write(new byte[]{0, 0, 0, 0});
                    fos.write("WAVEfmt ".getBytes());
                    fos.write(new byte[]{16, 0, 0, 0, 1, 0, 2, 0, 68, (byte)0xAC, 0, 0, 0x10, (byte)0xB1, 2, 0, 4, 0, 16, 0, 0x64, 0x61, 0x74, 0x61});

                    byte[] bufferAudio = new byte[4096];
                    long escritos = 44;
                    while (escritos < pesoDestino) {
                        randomizador.nextBytes(bufferAudio);
                        int aEscribir = (int) Math.min(bufferAudio.length, pesoDestino - escritos);
                        fos.write(bufferAudio, 0, aEscribir);
                        escritos += aEscribir;
                    }

                } else if (extension.equals("mp4") || extension.equals("3gp")) {
                    try (InputStream in = requireContext().getAssets().open("base_estatica.mp4")) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = in.read(buffer)) != -1) fos.write(buffer, 0, read);
                    } catch (Exception e) {
                        fos.write("FTYP_CORRUPTED_VIDEO_STREAM_MOCK_DATA".getBytes());
                    }

                } else if (extension.equals("pdf") || extension.equals("docx") || extension.equals("xlsx") || extension.equals("pptx")) {
                    byte[] bufferDocumento = new byte[4096];
                    long escritos = 0;
                    while (escritos < pesoDestino) {
                        randomizador.nextBytes(bufferDocumento);
                        int aEscribir = (int) Math.min(bufferDocumento.length, pesoDestino - escritos);
                        fos.write(bufferDocumento, 0, aEscribir);
                        escritos += aEscribir;
                    }

                } else {
                    fos.write("=== FINALSHIELD SECURE SANDBOX ENCRYPTED OBJECT ===\n".getBytes());
                }

                fos.flush();
                long pesoActual = archivoFalso.length();
                long bytesRestantes = pesoDestino - pesoActual;

                if (bytesRestantes > 0) {
                    byte[] rellenoBasura = new byte[8192];
                    while (bytesRestantes > 0) {
                        randomizador.nextBytes(rellenoBasura);
                        int aEscribir = (int) Math.min(rellenoBasura.length, bytesRestantes);
                        fos.write(rellenoBasura, 0, aEscribir);
                        bytesRestantes -= aEscribir;
                    }
                }
            }
            return archivoFalso;
        } catch (Exception e) {
            Log.e("FinalShield_Decoy", "Fallo construyendo el espejo simétrico", e);
            return null;
        }
    }

    private void solicitarHuellaParaDrive() {
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setPackage("com.google.android.apps.docs");

                try {
                    drivePickerLauncher.launch(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    Intent fallbackIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    fallbackIntent.setType("*/*");
                    drivePickerLauncher.launch(fallbackIntent);
                }
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinalShield - Google Drive")
                .setSubtitle("Autoriza la importación segura desde la nube")
                .setNegativeButtonText("Cancelar")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void procesarImportacionFalsaDesdeDrive(Uri uri) {
        String nombreArchivo = "FS_RECOVERED.enc";
        long tamanoBytes = 0;

        try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex != -1) nombreArchivo = cursor.getString(nameIndex);
                if (sizeIndex != -1) tamanoBytes = cursor.getLong(sizeIndex);
            }
        } catch (Exception ignored) {}

        if (!nombreArchivo.endsWith(".enc") && !nombreArchivo.startsWith("FS_PROTECTED_") && !nombreArchivo.startsWith("FS_SCAN_")) {
            Toast.makeText(getContext(), "Error: El archivo no pertenece a un contenedor protegido de FinalShield.", Toast.LENGTH_LONG).show();
            return;
        }

        cargaViewModel.resetear();

        // REDIRECCIÓN EN DRIVE DE LA VERSIÓN REAL: Apunta a R.id.archivosCifrados o al destino que refresca la lista legítima
        irACargaConDestino(R.id.archivosCifrados);

        final String finalNombre = nombreArchivo;
        final long finalTamano = tamanoBytes;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Thread.sleep(2500);

                File dirDescifrados = new File(requireContext().getFilesDir(), "descifrados");
                if (!dirDescifrados.exists()) dirDescifrados.mkdirs();

                String nombreLimpio = finalNombre.replace("FS_PROTECTED_", "").replace("FS_SCAN_", "").replace(".enc", "");
                if (!nombreLimpio.contains(".")) nombreLimpio += ".jpg";

                File archivoClaroDestino = new File(dirDescifrados, "desc_" + System.currentTimeMillis() + "_" + nombreLimpio);

                try (FileOutputStream fos = new FileOutputStream(archivoClaroDestino)) {
                    byte[] basura = new byte[4096];
                    Random r = new Random();
                    long escritos = 0;
                    while (escritos < finalTamano) {
                        r.nextBytes(basura);
                        int aEscribir = (int) Math.min(basura.length, finalTamano - escritos);
                        fos.write(basura, 0, aEscribir);
                        escritos += aEscribir;
                    }
                }

                ArchivoMetadata meta = new ArchivoMetadata();
                meta.setNombreArchivo(nombreLimpio);
                meta.setTamanioBytes(finalTamano);
                meta.setEstaCifrado(false);
                meta.setRutaLocalDescifrado(archivoClaroDestino.getAbsolutePath());
                meta.setIdUsuario(Integer.parseInt(idUsuarioLogueado));
                meta.setFechaSeleccion(new Date());
                meta.setOrigen(finalNombre.startsWith("FS_SCAN_") ? "ESCANEO" : "DRIVE_IMPORT");

                String ext = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(archivoClaroDestino).toString());
                meta.setTipoArchivo(MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase()));

                archivoDAO.insert(meta);

                new Handler(Looper.getMainLooper()).post(() -> {
                    cargaViewModel.terminarProceso();
                    clonarEstructuraReal();
                    Toast.makeText(getContext(), "¡Archivo restaurado y validado con éxito desde la nube!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    cargaViewModel.terminarProceso();
                    Toast.makeText(getContext(), "Fallo criptográfico local en el clúster remoto.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void ejecutarBloqueoReCifrado() {
        if (posicionSeleccionada == -1) return;
        dialogContainerCifrar.setVisibility(View.GONE);
        posicionSeleccionada = -1;
        Toast.makeText(getContext(), "Operación abortada por seguridad: Imposible re-cifrar un bloque binario desalineado. Riesgo de pérdida estructural.", Toast.LENGTH_LONG).show();
    }

    private void ejecutarAccionFalsaEliminar() {
        if (posicionSeleccionada == -1) return;
        dialogContainerEliminar.setVisibility(View.GONE);

        if (posicionSeleccionada < listaMetadata.size()) {
            listaMetadata.remove(posicionSeleccionada);
            adaptador.notifyDataSetChanged();
        }
        posicionSeleccionada = -1;
        Toast.makeText(getContext(), "Archivo triturado de la memoria caché preventiva.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemLongClick(View view, int position) {
        if (position >= 0 && position < listaMetadata.size()) {
            ArchivoMetadata archivo = listaMetadata.get(position);
            mostrarMenuOpcionesFalsas(archivo, position);
        }
    }

    private void mostrarMenuOpcionesFalsas(ArchivoMetadata archivo, int posicion) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_opciones_descifrado, null);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext()).create();
        dialog.setView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btnMenuDescargar).setOnClickListener(v -> {
            dialog.dismiss();
            ejecutarDescargaFalsaERR();
        });

        dialogView.findViewById(R.id.btnMenuCompartir).setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "Error crítico (0x800CC): Sincronización de llave RSA corrupta con el backend. Exportación abortada.", Toast.LENGTH_LONG).show();
        });

        dialogView.findViewById(R.id.btnMenuIntegridad).setOnClickListener(v -> {
            dialog.dismiss();
            calcularHashIntegridadFalso(archivo);
        });

        dialog.show();
    }

    private void ejecutarDescargaFalsaERR() {
        // ENGAÑO OPTIMIZADO: Apuntamos al fragmento hermano de la app real (archivosCifrados)
        // para que CargaProcesos no genere un bucle recursivo y rompa los hilos gráficos.
        irACargaConDestino(R.id.archivosCifrados);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                cargaViewModel.terminarProceso();
                Toast.makeText(getContext(), "HTTP Error 504: Gateway Timeout. El clúster remoto interrumpió la descarga por falta de respuesta del storage.", Toast.LENGTH_LONG).show();
            }
        }, 5000);
    }

    private void irACargaConDestino(int destinoId) {
        Bundle args = new Bundle();
        args.putInt("destino_final", destinoId);
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);
    }

    private void calcularHashIntegridadFalso(ArchivoMetadata archivo) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String hashCalculado = "e3b0c442" + Long.toHexString(new Random().nextLong());
            try {
                File clon = generarClonFalsoEspejo(archivo);
                if (clon != null && clon.exists()) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(clon)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = fis.read(buffer)) != -1) {
                            digest.update(buffer, 0, read);
                        }
                    }
                    byte[] hashBytes = digest.digest();
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : hashBytes) {
                        String hex = Integer.toHexString(0xff & b);
                        if (hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }
                    hashCalculado = hexString.toString();
                }
            } catch (Exception ignored) {}

            final String hashFinal = hashCalculado;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    boolean simularDesfase = (archivo.getNombreArchivo().hashCode() % 2 == 0);

                    String estadoSimulado = simularDesfase
                            ? "⚠️ ADVERTENCIA: Bloque binario modificado en tránsito o llave de sesión obsoleta."
                            : "✅ ÉXITO: Estructura del bloque verificada en espejo.";

                    String hashEsperado = simularDesfase
                            ? hashFinal.substring(0, Math.min(hashFinal.length(), 12)) + "...BLOCK_ERR"
                            : hashFinal;

                    String textoReporte = "Verificación de firma criptográfica del archivo local:\n\n" +
                            "🔍 ALGORITMO: SHA-256 \n\n" +
                            "🔑 HASH GENERADO:\n" + hashFinal + "\n\n" +
                            "🌐 HASH ESPERADO:\n" + hashEsperado + "\n\n" +
                            "ESTADO: " + estadoSimulado + "\n\n" +
                            "Identidad digital vinculada al usuario ID: " + idUsuarioLogueado + ".";

                    tvCuerpoIntegridad.setText(textoReporte);
                    mostrarDialogo(dialogContainerIntegridad, cardIntegridad, null);
                }
            });
        });
    }

    private void mostrarDialogo(LinearLayout container, View card, View buttons) {
        if (container == null) return;
        container.setVisibility(View.VISIBLE);
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.dialog_fade_in);
        if (card != null) card.startAnimation(anim);
    }

    private void ocultarDialogo(LinearLayout container, View card, View buttons) {
        if (container == null) return;

        if (card == null || getContext() == null) {
            container.setVisibility(View.GONE);
            posicionSeleccionada = -1;
            return;
        }

        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.dialog_fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                container.setVisibility(View.GONE);
                posicionSeleccionada = -1;
            }
        });
        card.startAnimation(anim);
    }

    @Override public void onDescifrarClick(int p) { posicionSeleccionada = p; mostrarDialogo(dialogContainerCifrar, cardIntegridad, null); }
    @Override public void onBorrarClick(int p) { posicionSeleccionada = p; mostrarDialogo(dialogContainerEliminar, cardIntegridad, null); }
    @Override public void onCambiarEstadoClick(int pos) {}

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);

        if (id == R.id.btnImportarDrive) {
            solicitarHuellaParaDrive();
        } else if (id == R.id.house) {
            nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        } else if (id == R.id.candadoclose) {
            nav.navigate(R.id.archivosCifrados2);
        } else if (id == R.id.carpeta) {
            nav.navigate(R.id.cifradoEscaneo2);
        } else if (id == R.id.archivo) {
            nav.navigate(R.id.archivosCifrados);
        } else if (id == R.id.mail) {
            nav.navigate(R.id.servivioCorreo);
        } else if (id == R.id.btnperfil) {
            nav.navigate(R.id.perfil2);
        } else if (id == R.id.candadopen) {
            clonarEstructuraReal();
        }
    }
}