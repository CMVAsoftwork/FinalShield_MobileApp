package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.io.InputStream;
import java.util.ArrayList;
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

    // Diálogos personalizados
    private LinearLayout dialogDescifrar, dialogEliminar;
    private int posicionSeleccionada = -1;

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

        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        listViewArchivos = v.findViewById(R.id.listacif);
        adaptador = new AdaptadorArchivos(getContext(), archivosSeleccionados, this);
        listViewArchivos.setAdapter(adaptador);

        // --- Inicializar Diálogos del XML ---
        dialogDescifrar = v.findViewById(R.id.dialogContainerDescifrar);
        dialogEliminar = v.findViewById(R.id.dialogContainerEliminar);

        // Botones Diálogo Descifrar
        v.findViewById(R.id.sidescifrar).setOnClickListener(view -> {
            dialogDescifrar.setVisibility(View.GONE);
            ejecutarDescifrado(posicionSeleccionada);
        });
        v.findViewById(R.id.nodescifrar).setOnClickListener(view -> dialogDescifrar.setVisibility(View.GONE));

        // Botones Diálogo Eliminar
        v.findViewById(R.id.sieliminar).setOnClickListener(view -> {
            dialogEliminar.setVisibility(View.GONE);
            ejecutarEliminacion(posicionSeleccionada);
        });
        v.findViewById(R.id.noeliminar).setOnClickListener(view -> dialogEliminar.setVisibility(View.GONE));

        // Botones de navegación
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
        final NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.cargaProcesos);

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
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (response.isSuccessful() && response.body() != null) {
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    for (Archivo a : response.body()) {
                                        ArchivoMetadata meta = new ArchivoMetadata(a);
                                        meta.setOrigen("ARCHIVOS");
                                        archivoDAO.insert(meta);
                                    }
                                    requireActivity().runOnUiThread(() -> {
                                        cargarDatosDesdeBD();
                                        navController.popBackStack(R.id.archivosCifrados2, false);
                                    });
                                });
                            } else {
                                navController.popBackStack();
                            }
                        });
                    }
                    @Override public void onFailure(Call<List<Archivo>> call, Throwable t) {
                        new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
                    }
                });
            } else {
                new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
            }
        });
    }

    @Override
    public void onDescifrarClick(int position) {
        this.posicionSeleccionada = position;
        dialogDescifrar.setVisibility(View.VISIBLE);
    }

    private void ejecutarDescifrado(int position) {
        if (position < 0 || position >= archivosSeleccionados.size()) return;
        ArchivoMetadata meta = archivosSeleccionados.get(position);

        final Context appContext = requireContext().getApplicationContext();
        final NavController navController = Navigation.findNavController(requireView());

        navController.navigate(R.id.cargaProcesos);

        archivoService.getAPI().descifrarArchivo(meta.getIdArchivoServidor()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            File dir = new File(appContext.getFilesDir(), "descifrados");
                            if (!dir.exists()) dir.mkdirs();

                            String mimeType = (meta.getTipoArchivo() != null) ? meta.getTipoArchivo() : "application/pdf";
                            String extension = mimeType.contains("/") ? mimeType.substring(mimeType.lastIndexOf("/") + 1) : mimeType;

                            String nombreFinal = "file_" + System.currentTimeMillis() + "." + extension;
                            File localFile = new File(dir, nombreFinal);

                            InputStream is = response.body().byteStream();
                            FileOutputStream fos = new FileOutputStream(localFile);
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                            fos.flush(); fos.close(); is.close();

                            meta.setEstaCifrado(false);
                            meta.setRutaLocalDescifrado(localFile.getAbsolutePath());
                            meta.setTamanioBytes(localFile.length());
                            archivoDAO.update(meta);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                NavOptions navOptions = new NavOptions.Builder()
                                        .setPopUpTo(R.id.cargaProcesos, true)
                                        .build();
                                navController.navigate(R.id.archivosDesifrados, null, navOptions);
                            });

                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
                }
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
            }
        });
    }

    @Override
    public void onBorrarClick(int position) {
        this.posicionSeleccionada = position;
        dialogEliminar.setVisibility(View.VISIBLE);
    }

    private void ejecutarEliminacion(int position) {
        if (position < 0 || position >= archivosSeleccionados.size()) return;
        ArchivoMetadata archivo = archivosSeleccionados.get(position);

        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    archivoDAO.delete(archivo);
                    requireActivity().runOnUiThread(() -> {
                        archivosSeleccionados.remove(position);
                        adaptador.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Eliminado correctamente", Toast.LENGTH_SHORT).show();
                    });
                });
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Error al borrar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnescanycifrar) filePickerLauncher.launch("*/*");
        else if (id == R.id.candadopen) Navigation.findNavController(v).navigate(R.id.archivosDesifrados);
        else if (id == R.id.carpeta) Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.candadoclose) cargarDatosDesdeBD();
        else if (id == R.id.btnperfil) Navigation.findNavController(v).navigate(R.id.perfil2);
        else if (id == R.id.mail) Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        else if (id == R.id.archivo) Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        else if (id == R.id.house) Navigation.findNavController(v).navigate(R.id.inicio);
    }

    @Override public void onItemClick(int position) {
        Toast.makeText(getContext(), "Archivo cifrado. Debe descifrarlo primero.", Toast.LENGTH_SHORT).show();
    }
    @Override public void onCambiarEstadoClick(int position) {}
}