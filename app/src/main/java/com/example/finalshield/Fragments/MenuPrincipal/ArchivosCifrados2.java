package com.example.finalshield.Fragments.MenuPrincipal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.Archivo;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.Util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    handleSelectedUris(uris);
                } else {
                    Toast.makeText(getContext(), "Selección de archivos cancelada", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_cifrados2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        Context context = getContext();
        if (context != null) {
            archivoService = new ArchivoService(context);
        }

        listViewArchivos = v.findViewById(R.id.listacif);
        adaptador = new AdaptadorArchivos(context, archivosSeleccionados, this);
        listViewArchivos.setAdapter(adaptador);

        ImageButton perfil = v.findViewById(R.id.btnperfil);
        ImageButton house = v.findViewById(R.id.house);
        ImageButton archivoBtn = v.findViewById(R.id.archivo);
        ImageButton candadclose = v.findViewById(R.id.candadoclose);
        ImageButton carpeta = v.findViewById(R.id.carpeta);
        ImageButton mail = v.findViewById(R.id.mail);
        ImageButton candadopen = v.findViewById(R.id.candadopen);
        Button escanarch = v.findViewById(R.id.btnescanycifrar);

        perfil.setOnClickListener(this);
        house.setOnClickListener(this);
        archivoBtn.setOnClickListener(this);
        candadclose.setOnClickListener(this);
        carpeta.setOnClickListener(this);
        mail.setOnClickListener(this);
        candadopen.setOnClickListener(this);
        escanarch.setOnClickListener(this);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = Room.databaseBuilder(requireContext().getApplicationContext(), AppDatabase.class, "archivo_db")
                    .fallbackToDestructiveMigration()  // ← Esto borra y recrea la DB si esquema cambió
                    .build();
            this.archivoDAO = db.archivoDAO();
            getActivity().runOnUiThread(this::fetchArchivosFromBackend);
        });
    }

    private void fetchArchivosFromBackend() {
        if (archivoService == null) return;

        archivoService.getAPI().getAllArchivos().enqueue(new Callback<List<Archivo>>() {
            @Override
            public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    archivosSeleccionados.clear();
                    for (Archivo arch : response.body()) {
                        archivosSeleccionados.add(new ArchivoMetadata(arch));
                    }
                    adaptador.notifyDataSetChanged();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        for (ArchivoMetadata meta : archivosSeleccionados) {
                            guardarArchivoEnPersistencia(meta);
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Error al cargar archivos: " + response.code(), Toast.LENGTH_SHORT).show();
                    cargarArchivosPersistidos();
                }
            }

            @Override
            public void onFailure(Call<List<Archivo>> call, Throwable t) {
                Toast.makeText(getContext(), "Fallo de conexión al cargar archivos", Toast.LENGTH_SHORT).show();
                cargarArchivosPersistidos();
            }
        });
    }

    private void cargarArchivosPersistidos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> locales = archivoDAO.getAll();
            getActivity().runOnUiThread(() -> {
                archivosSeleccionados.clear();
                archivosSeleccionados.addAll(locales);
                adaptador.notifyDataSetChanged();
            });
        });
    }

    private void guardarArchivoEnPersistencia(ArchivoMetadata archivo) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (archivo.getIdLocal() == 0) {
                archivoDAO.insert(archivo);
            } else {
                archivoDAO.update(archivo);
            }
        });
    }

    private void eliminarArchivoDePersistencia(ArchivoMetadata archivo) {
        Executors.newSingleThreadExecutor().execute(() -> archivoDAO.delete(archivo));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.carpeta) {
            Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        } else if (id == R.id.house) {
            Navigation.findNavController(v).navigate(R.id.inicio);
        } else if (id == R.id.candadoclose) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        } else if (id == R.id.candadopen) {
            Navigation.findNavController(v).navigate(R.id.archivosDesifrados);
        } else if (id == R.id.mail) {
            Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        } else if (id == R.id.archivo) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        } else if (id == R.id.btnperfil) {
            Navigation.findNavController(v).navigate(R.id.perfil2);
        } else if (id == R.id.btnescanycifrar) {
            filePickerLauncher.launch("*/*");
        }
    }

    private void handleSelectedUris(List<Uri> uris) {
        Context context = getContext();
        if (context == null) return;

        List<MultipartBody.Part> fileParts = new ArrayList<>();

        for (Uri uri : uris) {
            ArchivoMetadata metadata = getMetadataFromUri(context, uri);
            if (metadata != null) {
                // Validación duplicados por nombre
                boolean exists = archivosSeleccionados.stream().anyMatch(existing -> existing.getNombre().equals(metadata.getNombre()));
                if (exists) {
                    Toast.makeText(context, "Archivo " + metadata.getNombre() + " ya está cifrado.", Toast.LENGTH_SHORT).show();
                    continue;
                }

                guardarArchivoEnPersistencia(metadata);
                archivosSeleccionados.add(metadata);
                adaptador.notifyDataSetChanged();

                MultipartBody.Part filePart = FileUtils.prepareFilePartArchivo(context, uri);
                if (filePart != null) {
                    fileParts.add(filePart);
                }
            }
        }

        if (!fileParts.isEmpty()) {
            cifrarArchivos(fileParts);
        }
    }

    private ArchivoMetadata getMetadataFromUri(Context context, Uri uri) {
        String displayName = null;
        long size = 0;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex);
                }
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e("Metadata", "Error al obtener metadatos: " + e.getMessage());
        }
        if (displayName == null) {
            displayName = uri.getLastPathSegment();
        }
        return new ArchivoMetadata(displayName, uri, size, new Date(), false);
    }

    private void cifrarArchivos(List<MultipartBody.Part> fileParts) {
        Context context = getContext();
        if (context == null || archivoService == null) return;

        Toast.makeText(context, "Cifrando y subiendo archivos...", Toast.LENGTH_LONG).show();

        archivoService.getAPI().cifrarArchivos(fileParts).enqueue(new Callback<List<Archivo>>() {
            @Override
            public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Archivo> guardados = response.body();
                    for (int i = 0; i < guardados.size(); i++) {
                        Archivo arch = guardados.get(i);
                        ArchivoMetadata meta = archivosSeleccionados.get(archivosSeleccionados.size() - guardados.size() + i);
                        meta.setIdArchivoServidor(arch.getIdArchivo());
                        meta.setEstaCifrado(true);
                        meta.setTipoArchivo(arch.getTipoArchivo());
                        meta.setRutaServidor(arch.getRutaArchivo());
                        guardarArchivoEnPersistencia(meta);
                        descargarYGuardarLocal(meta, arch.getIdArchivo());
                    }
                    adaptador.notifyDataSetChanged();
                    Toast.makeText(context, "Archivos cifrados exitosamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Error al cifrar: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Archivo>> call, Throwable t) {
                Toast.makeText(context, "Fallo de conexión", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void descargarYGuardarLocal(ArchivoMetadata meta, int idArchivo) {
        archivoService.getAPI().descargarCifrado(idArchivo).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File encryptedDir = new File(requireContext().getFilesDir(), "encrypted");
                        if (!encryptedDir.exists()) encryptedDir.mkdirs();
                        File localFile = new File(encryptedDir, meta.getNombre() + ".enc");
                        FileOutputStream fos = new FileOutputStream(localFile);
                        InputStream is = response.body().byteStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close();
                        is.close();

                        meta.setRutaLocalEncriptada(localFile.getAbsolutePath());
                        guardarArchivoEnPersistencia(meta);
                    } catch (IOException e) {
                        Log.e("Download", "Error guardando local: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Download", "Fallo descargando: " + t.getMessage());
            }
        });
    }

    // DESCIFRADO CON CONFIRMACIÓN EN CANDADO
    @Override
    public void onDescifrarClick(int position) {
        ArchivoMetadata metaCifrado = archivosSeleccionados.get(position);

        new AlertDialog.Builder(requireContext())
                .setTitle("Descifrar archivo")
                .setMessage("¿Seguro que quieres descifrar '" + metaCifrado.getNombre() + "'?")
                .setPositiveButton("Sí", (dialog, which) -> descifrarArchivo(metaCifrado))
                .setNegativeButton("No", null)
                .show();
    }

    private void descifrarArchivo(ArchivoMetadata metaCifrado) {
        if (metaCifrado.getIdArchivoServidor() == null) {
            Toast.makeText(getContext(), "Error: No ID servidor", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Descifrando...", Toast.LENGTH_SHORT).show();

        archivoService.getAPI().descifrarArchivo(metaCifrado.getIdArchivoServidor()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    File descifradoDir = new File(requireContext().getFilesDir(), "descifrados");
                    if (!descifradoDir.exists()) descifradoDir.mkdirs();
                    File localDescifrado = new File(descifradoDir, metaCifrado.getNombre());

                    try (FileOutputStream fos = new FileOutputStream(localDescifrado)) {
                        InputStream is = response.body().byteStream();
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Error guardando descifrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Aquí el fix: Usa constructor vacío + setters manuales
                    ArchivoMetadata metaDescifrado = new ArchivoMetadata();  // Constructor vacío
                    metaDescifrado.setNombre(metaCifrado.getNombre());
                    metaDescifrado.setIdArchivoServidor(metaCifrado.getIdArchivoServidor());
                    metaDescifrado.setTamanioBytes(metaCifrado.getTamanioBytes());
                    metaDescifrado.setFechaSeleccion(metaCifrado.getFechaSeleccion());
                    metaDescifrado.setTipoArchivo(metaCifrado.getTipoArchivo());
                    metaDescifrado.setRutaServidor(metaCifrado.getRutaServidor());
                    // Copia otros campos si los usas (ej. uriOriginal si quieres)
                    metaDescifrado.setEstaCifrado(false);
                    metaDescifrado.setRutaLocalDescifrado(localDescifrado.getAbsolutePath());

                    guardarArchivoEnPersistencia(metaDescifrado);

                    Toast.makeText(getContext(), "Descifrado exitosamente", Toast.LENGTH_SHORT).show();

                    Navigation.findNavController(requireView()).navigate(R.id.archivosDesifrados);  // Cambia si tu action es diferente
                } else {
                    Toast.makeText(getContext(), "Error al descifrar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Fallo de conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBorrarClick(int position) {
        ArchivoMetadata archivo = archivosSeleccionados.get(position);
        Integer idServidor = archivo.getIdArchivoServidor();
        if (idServidor == null) {
            // Solo local
            if (archivo.getRutaLocalEncriptada() != null) {
                File localFile = new File(archivo.getRutaLocalEncriptada());
                if (localFile.exists()) localFile.delete();
            }
            archivosSeleccionados.remove(position);
            adaptador.notifyDataSetChanged();
            eliminarArchivoDePersistencia(archivo);
            Toast.makeText(getContext(), "Eliminado local: " + archivo.getNombre(), Toast.LENGTH_SHORT).show();
            return;
        }

        Context context = getContext();
        if (context == null || archivoService == null) return;

        archivoService.getAPI().borrarArchivo(idServidor).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (archivo.getRutaLocalEncriptada() != null) {
                        File localFile = new File(archivo.getRutaLocalEncriptada());
                        if (localFile.exists()) localFile.delete();
                    }
                    archivosSeleccionados.remove(position);
                    adaptador.notifyDataSetChanged();
                    eliminarArchivoDePersistencia(archivo);
                    Toast.makeText(context, "Eliminado completamente: " + archivo.getNombre(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Error al borrar en servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Fallo de conexión al borrar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCambiarEstadoClick(int position) {
        // No hace nada ahora
    }

    @Override
    public void onItemClick(int position) {
        ArchivoMetadata archivo = archivosSeleccionados.get(position);
        Context context = getContext();
        if (context == null || !archivo.isEstaCifrado()) {
            Toast.makeText(context, "Debes cifrar primero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (archivo.getRutaLocalEncriptada() != null) {
            File localFile = new File(archivo.getRutaLocalEncriptada());
            if (localFile.exists()) {
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", localFile);
                abrirArchivo(context, uri, archivo.getTipoArchivo());
                return;
            }
        }

        if (archivo.getIdArchivoServidor() == null) {
            Toast.makeText(context, "Error en ID", Toast.LENGTH_SHORT).show();
            return;
        }

        archivoService.getAPI().descargarCifrado(archivo.getIdArchivoServidor()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Uri tempUri = guardarContenidoTemporal(context, response.body().byteStream(), archivo.getNombre() + ".enc");
                    if (tempUri != null) {
                        abrirArchivo(context, tempUri, archivo.getTipoArchivo());
                    }
                } else {
                    Toast.makeText(context, "Error al descargar: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Fallo de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Uri guardarContenidoTemporal(Context context, InputStream inputStream, String nombre) {
        try {
            File tempFile = new File(context.getCacheDir(), nombre);
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", tempFile);
        } catch (IOException e) {
            Log.e("TempFile", "Error: " + e.getMessage());
            return null;
        }
    }

    private void abrirArchivo(Context context, Uri uri, String mimeType) {
        if (mimeType == null) mimeType = "*/*";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No app para abrir este archivo", Toast.LENGTH_SHORT).show();
        }
    }
}