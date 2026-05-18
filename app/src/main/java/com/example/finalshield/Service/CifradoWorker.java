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
import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Response;

public class CifradoWorker extends Worker {

    public CifradoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
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

        // --- ENLACE CON FIRMA CRIPTOGRÁFICA EXPLÍCITA REQUERIDA POR EL TARGET SDK 36 ---
        try {
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

        if (idUsuario == null || idUsuario.trim().isEmpty()) idUsuario = "18";

        if (urisInput == null || urisInput.length == 0) {
            cancelarNotificacionProgreso();
            return Result.failure();
        }

        ArchivoService archivoService = new ArchivoService(getApplicationContext());
        ArchivoDAO archivoDAO = AppDatabase.getInstance(getApplicationContext()).archivoDAO();
        List<File> temporalesCreados = new ArrayList<>();

        try {
            List<MultipartBody.Part> parts = new ArrayList<>();
            for (String uriStr : urisInput) {
                Uri uri = Uri.parse(uriStr);
                File archivoOriginalLimpio = FileUtils.getFileFromUri(getApplicationContext(), uri);

                if (archivoOriginalLimpio != null && archivoOriginalLimpio.exists()) {
                    String nombreFinalCache = (nombreVisualLimpio != null) ? nombreVisualLimpio : archivoOriginalLimpio.getName();
                    File archivoCifradoLocal = new File(getApplicationContext().getCacheDir(), "cif_" + nombreFinalCache);

                    SecurityUtils.cifrarArchivoLocal(archivoOriginalLimpio, archivoCifradoLocal, idUsuario);
                    temporalesCreados.add(archivoCifradoLocal);
                    archivoOriginalLimpio.delete();

                    MultipartBody.Part p = FileUtils.prepareFilePartDesdeFile(getApplicationContext(), archivoCifradoLocal);
                    if (p != null) parts.add(p);
                }
            }

            if (parts.isEmpty()) {
                limpiarTemporalesCache(temporalesCreados);
                cancelarNotificacionProgreso();
                return Result.failure();
            }

            Response<List<Archivo>> response = archivoService.getAPI().cifrarArchivos(parts).execute();
            limpiarTemporalesCache(temporalesCreados);

            if (response.isSuccessful() && response.body() != null) {
                for (Archivo a : response.body()) {
                    ArchivoMetadata meta = new ArchivoMetadata(a);
                    if (nombreVisualLimpio != null) meta.setNombreArchivo(nombreVisualLimpio);
                    meta.setOrigen("ARCHIVOS");
                    archivoDAO.insert(meta);
                }
                eliminarArchivosFisicos(urisInput);

                cancelarNotificacionProgreso();
                crearNotificacion("¡Archivos cifrados con éxito!", false);

                Log.d("FINALSHIELD_WORKER", "Cifrado completado.");
                return Result.success();
            } else {
                cancelarNotificacionProgreso();
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e("FINALSHIELD_WORKER", "Error crítico: " + e.getMessage());
            limpiarTemporalesCache(temporalesCreados);
            cancelarNotificacionProgreso();
            crearNotificacion("Error al cifrar archivos pesados", false);
            return Result.failure();
        }
    }

    private void eliminarArchivosFisicos(String[] uris) {
        for (String uriStr : uris) {
            try {
                Uri uri = Uri.parse(uriStr);
                DocumentFile doc = DocumentFile.fromSingleUri(getApplicationContext(), uri);
                if (doc != null && doc.exists()) {
                    doc.delete();
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