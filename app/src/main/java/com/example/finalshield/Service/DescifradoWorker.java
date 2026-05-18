package com.example.finalshield.Service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Util.SecurityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class DescifradoWorker extends Worker {

    public DescifradoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String CHANNEL_ID = "FinalShield_Descifrado";
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Descifrado de Archivos", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intentLaunch = new Intent(getApplicationContext(), com.example.finalshield.MainActivity.class);
        intentLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flagsPendingIntent = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 201, intentLaunch, flagsPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.candadoblan)
                .setContentTitle("FinalShield - Protección de Fondo")
                .setContentText("Descargando y descifrando bloque seguro...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(contentIntent);

        // --- ENLACE CON FIRMA CRIPTOGRÁFICA EXPLÍCITA REQUERIDA POR EL TARGET SDK 36 ---
        try {
            androidx.work.ForegroundInfo foregroundInfo;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                foregroundInfo = new androidx.work.ForegroundInfo(
                        201,
                        builder.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                );
            } else {
                foregroundInfo = new androidx.work.ForegroundInfo(201, builder.build());
            }
            setForegroundAsync(foregroundInfo).get();
        } catch (Exception e) {
            Log.e("FINALSHIELD_WORKER_DESC", "No se pudo establecer Foreground: " + e.getMessage());
        }

        int idArchivoServidor = getInputData().getInt("id_archivo_servidor", -1);
        String idUsuario = getInputData().getString("id_usuario_llave");
        String nombreOriginal = getInputData().getString("nombre_archivo_llave");
        String tipoMime = getInputData().getString("tipo_mime_llave");

        if (idArchivoServidor == -1) {
            cancelarNotificacionProgreso();
            return Result.failure();
        }

        if (idUsuario == null || idUsuario.trim().isEmpty()) {
            idUsuario = "18";
        }

        if (nombreOriginal == null || nombreOriginal.trim().isEmpty()) {
            nombreOriginal = "FS_RECOVERED.file";
        }

        ArchivoService archivoService = new ArchivoService(getApplicationContext());
        ArchivoDAO archivoDAO = AppDatabase.getInstance(getApplicationContext()).archivoDAO();

        try {
            Response<ResponseBody> response = archivoService.getAPI().descifrarArchivo(idArchivoServidor).execute();

            if (response.isSuccessful() && response.body() != null) {
                File dir = new File(getApplicationContext().getFilesDir(), "descifrados");
                if (!dir.exists()) dir.mkdirs();

                File tempCifradoDescargado = new File(dir, "temp_worker_desc_" + System.currentTimeMillis() + ".tmp");

                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(tempCifradoDescargado)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }

                String mime = tipoMime != null ? tipoMime : "application/octet-stream";
                File localFileReal = new File(dir, "desc_" + System.currentTimeMillis() + "_" + nombreOriginal);

                SecurityUtils.descifrarArchivoLocal(tempCifradoDescargado, localFileReal, idUsuario);
                tempCifradoDescargado.delete();

                ArchivoMetadata nuevoMeta = new ArchivoMetadata();
                nuevoMeta.setIdArchivoServidor(idArchivoServidor);
                nuevoMeta.setNombreArchivo(nombreOriginal);
                nuevoMeta.setEstaCifrado(false);
                nuevoMeta.setRutaLocalDescifrado(localFileReal.getAbsolutePath());
                nuevoMeta.setTamanioBytes(localFileReal.length());
                nuevoMeta.setTipoArchivo(mime);
                nuevoMeta.setOrigen("ARCHIVOS");
                nuevoMeta.setIdUsuario(Integer.parseInt(idUsuario));
                nuevoMeta.setFechaSeleccion(new Date());

                archivoDAO.insert(nuevoMeta);

                cancelarNotificacionProgreso();
                crearNotificacion("Archivo descifrado y disponible en la bóveda.", false);

                Log.d("FINALSHIELD_WORKER_DESC", "Descifrado asíncrono autónomo exitoso de: " + nombreOriginal);
                return Result.success();
            } else {
                cancelarNotificacionProgreso();
                return Result.retry();
            }

        } catch (Exception e) {
            Log.e("FINALSHIELD_WORKER_DESC", "Fallo crítico en segundo plano: " + e.getMessage());
            cancelarNotificacionProgreso();
            crearNotificacion("Error en la validación criptográfica de fondo", false);
            return Result.failure();
        }
    }

    private void crearNotificacion(String mensaje, boolean esProgreso) {
        String CHANNEL_ID = "FinalShield_Descifrado";
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Descifrado de Archivos", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        Intent intentLaunch = new Intent(getApplicationContext(), com.example.finalshield.MainActivity.class);
        intentLaunch.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (!esProgreso) {
            intentLaunch.putExtra("pantalla_destino", "DESCIFRADOS");
        }

        int flagsPendingIntent = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                esProgreso ? 201 : 202,
                intentLaunch,
                flagsPendingIntent
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.candadoblan)
                .setContentTitle("FinalShield - Protección de Fondo")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(esProgreso)
                .setContentIntent(contentIntent)
                .setAutoCancel(!esProgreso);

        int notificationId = esProgreso ? 201 : 202;
        manager.notify(notificationId, builder.build());
    }

    private void cancelarNotificacionProgreso() {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(201);
    }
}