package com.example.finalshield.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.Archivo;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Util.FileUtils;
import com.example.finalshield.Util.SecurityUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Response;

public class CifradoWorker extends Worker {

    public CifradoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // 🎯 INDISPENSABLE PARA DESTRUIR EL LÍMITE DE LOS 10 MINUTOS DE ANDROID
    // Le avisa al sistema operativo que este hilo de ejecución tiene inmunidad de tiempo para transferencias pesadas.
    @NonNull
    @Override
    public com.google.common.util.concurrent.ListenableFuture<androidx.work.ForegroundInfo> getForegroundInfoAsync() {
        androidx.work.impl.utils.futures.SettableFuture<androidx.work.ForegroundInfo> future = androidx.work.impl.utils.futures.SettableFuture.create();

        String CHANNEL_ID = "FinalShield_Cifrado";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.candadoblan)
                .setContentTitle("FinalShield - Canal Seguro")
                .setContentText("Transmitiendo datos protegidos a la bóveda...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            future.set(new androidx.work.ForegroundInfo(101, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC));
        } else {
            future.set(new androidx.work.ForegroundInfo(101, builder.build()));
        }
        return future;
    }

    @NonNull
    @Override
    public Result doWork() {
        String CHANNEL_ID = "FinalShield_Cifrado";
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Cifrado de Archivos", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intentLaunch = new Intent(getApplicationContext(), com.example.finalshield.MainActivity.class);
        intentLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flagsPendingIntent = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 101, intentLaunch, flagsPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.candadoblan)
                .setContentTitle("FinalShield - Protección")
                .setContentText("Cifrando archivos en segundo plano...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(contentIntent);

        try {
            // Inicialización atómica de Foreground heredando la especificación del sistema de sincronización prolongada
            androidx.work.ForegroundInfo foregroundInfo;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                foregroundInfo = new androidx.work.ForegroundInfo(
                        101,
                        builder.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                );
            } else {
                foregroundInfo = new androidx.work.ForegroundInfo(101, builder.build());
            }
            setForegroundAsync(foregroundInfo).get();
        } catch (Exception e) {
            Log.e("FINALSHIELD_WORKER", "No se pudo establecer Foreground: " + e.getMessage());
        }

        String[] urisInput = getInputData().getStringArray("uris_llave");
        String idUsuario = getInputData().getString("id_usuario_llave");
        String nombreVisualLimpio = getInputData().getString("nombre_visual_limpio");

        String origenBoveda = getInputData().getString("origen_boveda");
        if (origenBoveda == null || origenBoveda.trim().isEmpty()) {
            origenBoveda = "ARCHIVOS";
        }

        if (idUsuario == null || idUsuario.trim().isEmpty()) idUsuario = "18";

        if (urisInput == null || urisInput.length == 0) {
            cancelarNotificacionProgreso();
            return Result.failure();
        }

        ArchivoService archivoService = new ArchivoService(getApplicationContext());
        ArchivoDAO archivoDAO = AppDatabase.getInstance(getApplicationContext()).archivoDAO();
        List<File> temporalesCreados = new ArrayList<>();

        File backupLocalParaContingencia = null;

        try {
            List<MultipartBody.Part> parts = new ArrayList<>();
            for (String pathOrUri : urisInput) {
                File archivoOriginalLimpio = null;

                if (pathOrUri.startsWith("/")) {
                    archivoOriginalLimpio = new File(pathOrUri);
                } else {
                    Uri uri = Uri.parse(pathOrUri);
                    archivoOriginalLimpio = FileUtils.getFileFromUri(getApplicationContext(), uri);
                }

                if (archivoOriginalLimpio != null && archivoOriginalLimpio.exists() && archivoOriginalLimpio.length() > 0) {
                    String nombreFinalCache = (nombreVisualLimpio != null) ? nombreVisualLimpio : archivoOriginalLimpio.getName();
                    File archivoCifradoLocal = new File(getApplicationContext().getCacheDir(), "cif_" + nombreFinalCache);

                    SecurityUtils.cifrarArchivoLocal(archivoOriginalLimpio, archivoCifradoLocal, idUsuario);
                    temporalesCreados.add(archivoCifradoLocal);

                    backupLocalParaContingencia = archivoCifradoLocal;

                    MultipartBody.Part p = FileUtils.prepareFilePartDesdeFile(getApplicationContext(), archivoCifradoLocal);
                    if (p != null) parts.add(p);
                } else {
                    Log.e("FINALSHIELD_WORKER", "Insumo de cifrado corrupto o ilegible: " + pathOrUri);
                }
            }

            if (parts.isEmpty()) {
                limpiarTemporalesCache(temporalesCreados);
                cancelarNotificacionProgreso();
                return Result.failure();
            }

            Response<List<Archivo>> response = archivoService.getAPI().cifrarArchivos(parts).execute();

            if (response.isSuccessful() && response.body() != null) {
                limpiarTemporalesCache(temporalesCreados);

                for (Archivo a : response.body()) {
                    ArchivoMetadata meta = new ArchivoMetadata(a);
                    if (nombreVisualLimpio != null) meta.setNombreArchivo(nombreVisualLimpio);

                    meta.setOrigen(origenBoveda);
                    archivoDAO.insert(meta);
                }

                eliminarArchivosFisicos(urisInput);
                cancelarNotificacionProgreso();
                crearNotificacion("¡Archivos cifrados y respaldados con éxito!", false);

                Log.d("FINALSHIELD_WORKER", "Cifrado completo en la nube con origen: " + origenBoveda);
                return Result.success();
            } else {
                cancelarNotificacionProgreso();
                throw new java.io.IOException("Error en respuesta del servidor o timeout de red.");
            }

        } catch (Exception e) {
            Log.e("FINALSHIELD_WORKER", "Error de transmisión detectado (Salvando datos locales): " + e.getMessage());
            cancelarNotificacionProgreso();

            // 🎯 SISTEMA DE CONTINGENCIA INTEGRAL CON CANDADOS DE ENRUTAMIENTO EXTREMOS
            try {
                if (backupLocalParaContingencia != null && backupLocalParaContingencia.exists() && backupLocalParaContingencia.length() > 0) {
                    File dirCifradosLocales = new File(getApplicationContext().getFilesDir(), "cifrados_locales");
                    if (!dirCifradosLocales.exists()) dirCifradosLocales.mkdirs();

                    File destinoFisicoFinal = new File(dirCifradosLocales, backupLocalParaContingencia.getName());

                    if (backupLocalParaContingencia.renameTo(destinoFisicoFinal)) {
                        backupLocalParaContingencia = destinoFisicoFinal;
                    }

                    ArchivoMetadata metaLocal = new ArchivoMetadata();
                    String nombreLimpio = destinoFisicoFinal.getName().replace("cif_", "");

                    // 🎯 MARCA VISUAL INQUEBRANTABLE PARA EVITAR DESVÍOS ENTRE PANTALLAS
                    // Si el nombre lleva marcas del escáner de galería o cámara, forzamos pasaporte criptográfico de escaneo
                    if (nombreLimpio.toUpperCase().contains("SCAN") || nombreLimpio.toUpperCase().contains("GAL") || "ESCANEO".equals(origenBoveda)) {
                        metaLocal.setNombreArchivo("FS_SCAN_" + nombreLimpio.replace("FS_SCAN_", ""));
                        metaLocal.setOrigen("ESCANEO");
                    } else {
                        metaLocal.setNombreArchivo(nombreLimpio);
                        metaLocal.setOrigen(origenBoveda);
                    }

                    metaLocal.setTamanioBytes(destinoFisicoFinal.length());
                    metaLocal.setEstaCifrado(true);
                    metaLocal.setRutaLocalEncriptada(destinoFisicoFinal.getAbsolutePath());
                    metaLocal.setIdUsuario(Integer.parseInt(idUsuario));
                    metaLocal.setFechaSeleccion(new Date());
                    metaLocal.setTipoArchivo("application/pdf");

                    archivoDAO.insert(metaLocal);

                    eliminarArchivosFisicos(urisInput);
                    limpiarTemporalesCache(temporalesCreados);

                    crearNotificacion("Protección local completada (Servidor fuera de línea)", false);
                    Log.d("FINALSHIELD_WORKER", "Rescate completado con enrutamiento de contingencia fijo.");
                    return Result.success();
                }
            } catch (Exception ex) {
                Log.e("FINALSHIELD_WORKER", "Fallo severo en el resguardo de contingencia local: " + ex.getMessage());
            }

            limpiarTemporalesCache(temporalesCreados);
            crearNotificacion("Error crítico al cifrar archivo pesado", false);
            return Result.failure();
        }
    }

    private void eliminarArchivosFisicos(String[] uris) {
        for (String uriStr : uris) {
            try {
                if (uriStr.startsWith("/")) {
                    File f = new File(uriStr);
                    if (f.exists()) f.delete();
                } else {
                    Uri uri = Uri.parse(uriStr);
                    DocumentFile doc = DocumentFile.fromSingleUri(getApplicationContext(), uri);
                    if (doc != null && doc.exists()) doc.delete();
                }
            } catch (Exception e) {
                Log.e("FINALSHIELD_WORKER", "No se pudo borrar original: " + uriStr);
            }
        }
    }

    private void limpiarTemporalesCache(List<File> archivosTemporales) {
        for (File f : archivosTemporales) {
            if (f != null && f.exists()) {
                f.delete();
            }
        }
    }

    private void crearNotificacion(String mensaje, boolean esProgreso) {
        String CHANNEL_ID = "FinalShield_Cifrado";
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Cifrado de Archivos", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intentLaunch = new Intent(getApplicationContext(), com.example.finalshield.MainActivity.class);
        intentLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (!esProgreso) {
            intentLaunch.putExtra("pantalla_destino", "CIFRADOS");
        }

        int flagsPendingIntent = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                esProgreso ? 101 : 102,
                intentLaunch,
                flagsPendingIntent
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.candadoblan)
                .setContentTitle("FinalShield - Protección")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(esProgreso)
                .setContentIntent(contentIntent)
                .setAutoCancel(!esProgreso);

        int notificationId = esProgreso ? 101 : 102;
        manager.notify(notificationId, builder.build());
    }

    private void cancelarNotificacionProgreso() {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(101);
    }
}