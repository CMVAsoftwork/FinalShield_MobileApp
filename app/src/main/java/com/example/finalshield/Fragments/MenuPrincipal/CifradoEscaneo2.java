package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CifradoEscaneo2 extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private final List<ArchivoMetadata> listaArchivos = new ArrayList<>();
    private ListView listView;
    private AdaptadorArchivos adaptador;
    private ArchivoDAO archivoDAO;
    private ArchivoService archivoService;

    // Referencias a los diálogos del Layout
    private LinearLayout dialogDescifrar, dialogEliminar;
    private int posicionSeleccionada = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cifrado_escaneo2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        //getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        listView = v.findViewById(R.id.listaescan);
        adaptador = new AdaptadorArchivos(getContext(), listaArchivos, this);
        listView.setAdapter(adaptador);

        // --- Inicialización de Diálogos ---
        dialogDescifrar = v.findViewById(R.id.dialogContainer);
        dialogEliminar = v.findViewById(R.id.dialogContainer2);

        // Botones de Confirmación Descifrar
        v.findViewById(R.id.sieliminar).setOnClickListener(view -> { // ID 'sieliminar' en tu XML para descifrar
            dialogDescifrar.setVisibility(View.GONE);
            ejecutarDescifrado(posicionSeleccionada);
        });
        v.findViewById(R.id.noeliminar).setOnClickListener(view -> dialogDescifrar.setVisibility(View.GONE));

        // Botones de Confirmación Eliminar
        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> {
            dialogEliminar.setVisibility(View.GONE);
            ejecutarEliminacion(posicionSeleccionada);
        });
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> dialogEliminar.setVisibility(View.GONE));
        // ----------------------------------

        // Listeners de botones de navegación
        v.findViewById(R.id.scan).setOnClickListener(this);
        v.findViewById(R.id.btnperfil).setOnClickListener(this);
        v.findViewById(R.id.house).setOnClickListener(this);
        v.findViewById(R.id.archivo).setOnClickListener(this);
        v.findViewById(R.id.candadoclose).setOnClickListener(this);
        v.findViewById(R.id.carpeta).setOnClickListener(this);
        v.findViewById(R.id.mail).setOnClickListener(this);
        v.findViewById(R.id.candadopen).setOnClickListener(this);

        cargarDatosDesdeBD();
    }

    private void cargarDatosDesdeBD() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> archivosBD = archivoDAO.getAllCifradosEscaneo();
            for (ArchivoMetadata m : archivosBD) {
                if (m.getFechaSeleccion() == null) m.setFechaSeleccion(new Date());
                if (m.getOrigen() == null) m.setOrigen("ESCANEO");
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    listaArchivos.clear();
                    listaArchivos.addAll(archivosBD);
                    adaptador.notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    public void onDescifrarClick(int position) {
        this.posicionSeleccionada = position;
        dialogDescifrar.setVisibility(View.VISIBLE);
    }

    private void ejecutarDescifrado(int position) {
        if (position < 0) return;
        ArchivoMetadata meta = listaArchivos.get(position);
        if (meta.getIdArchivoServidor() == null) return;

        final NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.cargaProcesos);

        archivoService.getAPI().descifrarArchivo(meta.getIdArchivoServidor()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            File dir = new File(requireContext().getFilesDir(), "descifrados");
                            if (!dir.exists()) dir.mkdirs();

                            String nombreLimpio = meta.getNombre().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
                            File localFile = new File(dir, nombreLimpio);

                            try (InputStream is = response.body().byteStream();
                                 FileOutputStream fos = new FileOutputStream(localFile)) {
                                byte[] buffer = new byte[8192];
                                int read;
                                while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                                fos.flush();
                            }

                            meta.setEstaCifrado(false);
                            meta.setTamanioBytes(localFile.length());
                            meta.setRutaLocalDescifrado(localFile.getAbsolutePath());
                            meta.setFechaSeleccion(new Date());
                            meta.setOrigen("ESCANEO");

                            archivoDAO.update(meta);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                NavOptions navOptions = new NavOptions.Builder()
                                        .setPopUpTo(R.id.cargaProcesos, true)
                                        .build();
                                navController.navigate(R.id.archivosDesifrados, null, navOptions);
                            });

                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                navController.popBackStack();
                                Toast.makeText(getContext(), "Error al guardar archivo", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                new Handler(Looper.getMainLooper()).post(() -> navController.popBackStack());
            }
        });
    }

    @Override public void onBorrarClick(int position) {
        this.posicionSeleccionada = position;
        dialogEliminar.setVisibility(View.VISIBLE);
    }

    private void ejecutarEliminacion(int position) {
        if (position < 0) return;
        ArchivoMetadata archivo = listaArchivos.get(position);
        archivoService.getAPI().borrarArchivo(archivo.getIdArchivoServidor()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    archivoDAO.delete(archivo);
                    requireActivity().runOnUiThread(() -> {
                        listaArchivos.remove(position);
                        adaptador.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                    });
                });
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Error al conectar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.scan) Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        else if (id == R.id.house) Navigation.findNavController(v).navigate(R.id.inicio);
        else if (id == R.id.candadoclose) Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        else if (id == R.id.candadopen) Navigation.findNavController(v).navigate(R.id.archivosDesifrados);
        else if (id == R.id.archivo) Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        else if (id == R.id.btnperfil) Navigation.findNavController(v).navigate(R.id.perfil2);
        else if (id == R.id.mail) Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        else if (id == R.id.carpeta) cargarDatosDesdeBD();
    }

    @Override public void onItemClick(int position) {
        Toast.makeText(getContext(), "Archivo protegido. Descifre primero.", Toast.LENGTH_SHORT).show();
    }

    @Override public void onCambiarEstadoClick(int position) {}
}