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

import android.os.Handler;
import android.os.Looper;
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
                    enviarAlServidor(uris);
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

        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        listViewArchivos = v.findViewById(R.id.listacif);
        adaptador = new AdaptadorArchivos(getContext(), archivosSeleccionados, this);
        listViewArchivos.setAdapter(adaptador);

        // Botones de navegación y acción
        v.findViewById(R.id.btnescanycifrar).setOnClickListener(this);
        v.findViewById(R.id.candadoclose).setOnClickListener(this);
        v.findViewById(R.id.candadopen).setOnClickListener(this);
        v.findViewById(R.id.carpeta).setOnClickListener(this);
        v.findViewById(R.id.house).setOnClickListener(this);
        v.findViewById(R.id.btnperfil).setOnClickListener(this);
        v.findViewById(R.id.mail).setOnClickListener(this);
        v.findViewById(R.id.archivo).setOnClickListener(this);

        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Filtra solo los que pertenecen a esta pantalla y están cifrados
            List<ArchivoMetadata> archivosBD = archivoDAO.getAllCifradosPrincipales();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    archivosSeleccionados.clear();
                    archivosSeleccionados.addAll(archivosBD);
                    adaptador.notifyDataSetChanged();
                });
            }
        });
    }

    private void enviarAlServidor(List<Uri> uris) {
        Toast.makeText(getContext(), "Cifrando y subiendo...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<MultipartBody.Part> parts = new ArrayList<>();
            for (Uri u : uris) {
                MultipartBody.Part p = FileUtils.prepareFilePartArchivo(requireContext(), u);
                if (p != null) parts.add(p);
            }

            if (!parts.isEmpty()) {
                archivoService.getAPI().cifrarArchivos(parts).enqueue(new Callback<List<Archivo>>() {
                    @Override
                    public void onResponse(Call<List<Archivo>> call, Response<List<Archivo>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                for (Archivo a : response.body()) {
                                    // Creamos la metadata con los datos que el servidor nos devuelve (ID y Tamaño)
                                    ArchivoMetadata meta = new ArchivoMetadata(a);
                                    meta.setOrigen("ARCHIVOS");
                                    archivoDAO.insert(meta);
                                }
                                requireActivity().runOnUiThread(() -> cargarDatosDesdeBD());
                            });
                        }
                    }
                    @Override public void onFailure(Call<List<Archivo>> call, Throwable t) {
                        requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        });
    }

    @Override
    public void onDescifrarClick(int position) {
        if (position < 0 || position >= archivosSeleccionados.size()) return;
        ArchivoMetadata meta = archivosSeleccionados.get(position);
        if (meta == null) return;

        final Context appContext = requireContext().getApplicationContext();
        final View currentView = getView();
        if (currentView == null) return;

        final androidx.navigation.NavController navController = Navigation.findNavController(currentView);

        Toast.makeText(appContext, "Descifrando...", Toast.LENGTH_SHORT).show();

        archivoService.getAPI().descifrarArchivo(meta.getIdArchivoServidor()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            File dir = new File(appContext.getFilesDir(), "descifrados");
                            if (!dir.exists()) dir.mkdirs();

                            // --- CORRECCIÓN AQUÍ: Limpiar la barra '/' del tipoArchivo ---
                            String mimeType = (meta.getTipoArchivo() != null) ? meta.getTipoArchivo() : "application/pdf";
                            String extension;

                            if (mimeType.contains("/")) {
                                // De "image/jpeg" extraemos solo "jpeg"
                                extension = mimeType.substring(mimeType.lastIndexOf("/") + 1);
                            } else {
                                extension = mimeType;
                            }

                            // Nombre de archivo seguro sin caracteres prohibidos
                            String nombreFinal = "file_" + System.currentTimeMillis() + "." + extension;
                            File localFile = new File(dir, nombreFinal);

                            // Escritura
                            InputStream is = response.body().byteStream();
                            FileOutputStream fos = new FileOutputStream(localFile);
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, read);
                            }
                            fos.flush();
                            fos.close();
                            is.close();

                            // Actualización DB
                            meta.setEstaCifrado(false);
                            meta.setRutaLocalDescifrado(localFile.getAbsolutePath());
                            meta.setTamanioBytes(localFile.length());
                            archivoDAO.update(meta);

                            // NAVEGACIÓN
                            new Handler(Looper.getMainLooper()).post(() -> {
                                navController.navigate(R.id.archivosDesifrados);
                            });

                        } catch (Exception e) {
                            Log.e("DESCIFRADO_ERROR", "Error: " + e.getMessage());
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(appContext, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(appContext, "Fallo de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnescanycifrar) filePickerLauncher.launch("*/*");
        else if (id == R.id.candadopen) Navigation.findNavController(v).navigate(R.id.archivosDesifrados);
        else if (id == R.id.carpeta) Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.house) Navigation.findNavController(v).navigate(R.id.inicio);
        else if (id == R.id.candadoclose) cargarDatosDesdeBD();
        else if (id == R.id.btnperfil) Navigation.findNavController(v).navigate(R.id.perfil2);
        else if (id == R.id.mail) Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        else if (id == R.id.archivo) Navigation.findNavController(v).navigate(R.id.archivosCifrados);
    }

    @Override
    public void onBorrarClick(int position) {
        ArchivoMetadata archivo = archivosSeleccionados.get(position);
        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    archivoDAO.delete(archivo);
                    requireActivity().runOnUiThread(() -> {
                        archivosSeleccionados.remove(position);
                        adaptador.notifyDataSetChanged();
                    });
                });
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override public void onItemClick(int position) {
        Toast.makeText(getContext(), "Archivo cifrado. Debe descifrarlo primero.", Toast.LENGTH_SHORT).show();
    }
    @Override public void onCambiarEstadoClick(int position) {}
}