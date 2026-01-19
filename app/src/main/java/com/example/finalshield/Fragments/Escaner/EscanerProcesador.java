package com.example.finalshield.Fragments.Escaner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

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

    public static void generarPdfYEnviar(Context context, List<Uri> uris, ArchivoService service,
                                         ArchivoDAO dao, Runnable onSuccess) {
        if (uris.isEmpty()) {
            Toast.makeText(context, "No hay imágenes para procesar", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            PdfDocument document = new PdfDocument();
            try {
                for (int i = 0; i < uris.size(); i++) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uris.get(i));

                    if (bitmap.getWidth() > 1600 || bitmap.getHeight() > 1600) {
                        float scale = Math.min(1600f / bitmap.getWidth(), 1600f / bitmap.getHeight());
                        bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*scale), (int)(bitmap.getHeight()*scale), true);
                    }

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    Canvas canvas = page.getCanvas();
                    canvas.drawBitmap(bitmap, 0, 0, null);
                    document.finishPage(page);
                }

                File pdfFile = new File(context.getCacheDir(), "SCAN_" + System.currentTimeMillis() + ".pdf");
                FileOutputStream fos = new FileOutputStream(pdfFile);
                document.writeTo(fos);
                document.close();
                fos.close();

                RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), pdfFile);
                MultipartBody.Part body = MultipartBody.Part.createFormData("archivo", pdfFile.getName(), requestFile);

                service.getAPI().cifrarArchivos(Collections.singletonList(body)).enqueue(new Callback<List<Archivo>>() {
                    @Override
                    public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                Archivo a = response.body().get(0);

                                // Creamos el objeto limpio
                                ArchivoMetadata meta = new ArchivoMetadata();

                                // Asignamos valores usando los nombres exactos de tu clase Archivo
                                meta.setIdArchivoServidor(a.getIdArchivo());
                                meta.setNombre(a.getNombreArchivo());

                                // CORRECCIÓN AQUÍ: getTamano() sin "i" y sin validación null (es long primitivo)
                                meta.setTamanioBytes(a.getTamano());

                                meta.setFechaSeleccion(new Date());
                                meta.setRutaServidor(a.getRutaArchivo());
                                meta.setTipoArchivo(a.getTipoArchivo());
                                meta.setEstaCifrado(true);
                                meta.setOrigen("ESCANEO");

                                dao.insert(meta);

                                new Handler(Looper.getMainLooper()).post(onSuccess);
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(context, "Error en el servidor", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Archivo>> call, Throwable t) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(context, "Fallo de red: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}