package com.example.finalshield.Fragments.Escaner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.Archivo;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.Service.ArchivoService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EscanerProcesador {

    // en el hilo secundario provisto por el disparador del fragmento de la cámara.
    public static void compilarPdfLocalDesdeUris(Context context, List<Uri> uris, File fileDestino) throws Exception {
        if (uris == null || uris.isEmpty()) {
            throw new IllegalArgumentException("No hay imágenes para el procesamiento local");
        }

        PdfDocument document = new PdfDocument();
        FileOutputStream fos = null;

        try {
            for (int i = 0; i < uris.size(); i++) {
                Uri uri = uris.get(i);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

                // Redimensionar para optimizar peso en transmisión asíncrona
                if (bitmap.getWidth() > 1600 || bitmap.getHeight() > 1600) {
                    float scale = Math.min(1600f / bitmap.getWidth(), 1600f / bitmap.getHeight());
                    bitmap = Bitmap.createScaledBitmap(bitmap,
                            (int) (bitmap.getWidth() * scale),
                            (int) (bitmap.getHeight() * scale),
                            true);
                }

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawBitmap(bitmap, 0, 0, null);
                document.finishPage(page);

                bitmap.recycle(); // Liberación inmediata de RAM por ráfagas pesadas
            }

            fos = new FileOutputStream(fileDestino);
            document.writeTo(fos);
            Log.d("FINALSHIELD_PROCESSOR", "PDF local compilado con éxito. Peso: " + fileDestino.length() + " bytes.");

        } finally {
            if (document != null) {
                document.close();
            }
            if (fos != null) {
                try { fos.close(); } catch (IOException ignored) {}
            }
        }
    }

    public static void generarPdfYEnviar(
            Context context,
            List<Uri> uris,
            ArchivoService service,
            ArchivoDAO dao,
            Runnable onSuccess,
            java.util.function.Consumer<String> onError
    ) {
        if (uris == null || uris.isEmpty()) {
            onError.accept("No hay imágenes para procesar");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            File pdfFile = null;
            PdfDocument document = null;
            FileOutputStream fos = null;

            try {
                document = new PdfDocument();

                for (int i = 0; i < uris.size(); i++) {
                    Uri uri = uris.get(i);
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

                    if (bitmap.getWidth() > 1600 || bitmap.getHeight() > 1600) {
                        float scale = Math.min(1600f / bitmap.getWidth(), 1600f / bitmap.getHeight());
                        bitmap = Bitmap.createScaledBitmap(bitmap,
                                (int) (bitmap.getWidth() * scale),
                                (int) (bitmap.getHeight() * scale),
                                true);
                    }

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                            bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    document.finishPage(page);

                    bitmap.recycle();
                }

                pdfFile = new File(context.getCacheDir(), "SCAN_" + System.currentTimeMillis() + ".pdf");
                fos = new FileOutputStream(pdfFile);
                document.writeTo(fos);

                RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), pdfFile);
                MultipartBody.Part body = MultipartBody.Part.createFormData("archivo", pdfFile.getName(), requestFile);

                File finalPdfFile = pdfFile;
                service.getAPI().cifrarArchivos(Collections.singletonList(body))
                        .enqueue(new Callback<List<Archivo>>() {
                            @Override
                            public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                    Archivo a = response.body().get(0);

                                    ArchivoMetadata meta = new ArchivoMetadata();
                                    meta.setIdArchivoServidor(a.getIdArchivo());
                                    // Se corrigió a .setNombreArchivo para empalmar limpio con tu entidad Room corregida
                                    meta.setNombreArchivo(a.getNombreArchivo());
                                    meta.setTamanioBytes(a.getTamano());
                                    meta.setFechaSeleccion(new Date());
                                    meta.setRutaServidor(a.getRutaArchivo());
                                    meta.setTipoArchivo(a.getTipoArchivo());
                                    meta.setEstaCifrado(true);
                                    meta.setOrigen("ESCANEO");

                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        try {
                                            dao.insert(meta);
                                            new Handler(Looper.getMainLooper()).post(onSuccess);
                                        } catch (Exception e) {
                                            Log.e("DB_ERROR", "Error insertando metadata", e);
                                            new Handler(Looper.getMainLooper()).post(() ->
                                                    onError.accept("Error al guardar el registro local"));
                                        }
                                    });
                                } else {
                                    String msg = "Error del servidor: " + response.code() +
                                            (response.message() != null ? " - " + response.message() : "");
                                    new Handler(Looper.getMainLooper()).post(() -> onError.accept(msg));
                                }

                                if (finalPdfFile != null && finalPdfFile.exists()) finalPdfFile.delete();
                            }

                            @Override
                            public void onFailure(Call<List<Archivo>> call, Throwable t) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                        onError.accept("Fallo de conexión: " + t.getMessage()));

                                if (finalPdfFile != null && finalPdfFile.exists()) finalPdfFile.delete();
                            }
                        });

            } catch (IOException | SecurityException e) {
                Log.e("PDF_ERROR", "Error procesando imágenes/PDF", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        onError.accept("No se pudo generar el PDF: " + e.getMessage()));

                if (pdfFile != null && pdfFile.exists()) pdfFile.delete();
            } finally {
                if (document != null) document.close();
                if (fos != null) {
                    try { fos.close(); } catch (IOException ignored) {}
                }
            }
        });
    }
}