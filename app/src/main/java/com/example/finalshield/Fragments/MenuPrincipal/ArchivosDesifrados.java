package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ArchivosDesifrados extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private ListView listView;
    private AdaptadorArchivos adaptador;
    private final List<ArchivoMetadata> listaMetadata = new ArrayList<>();
    private ArchivoDAO archivoDAO;

    private LinearLayout dialogContainerCifrar;
    private LinearLayout dialogContainerEliminar;
    private int posicionSeleccionada = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_desifrados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();
        listView = v.findViewById(R.id.listadesc);
        dialogContainerCifrar = v.findViewById(R.id.dialogContainer);
        dialogContainerEliminar = v.findViewById(R.id.dialogContainer2);

        adaptador = new AdaptadorArchivos(getContext(), listaMetadata, this);
        listView.setAdapter(adaptador);

        // Listeners de botones de diálogo
        v.findViewById(R.id.sicifrar).setOnClickListener(view -> ejecutarAccionCifrar());
        v.findViewById(R.id.nocifrar).setOnClickListener(view -> ocultarDialogos());
        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> ejecutarAccionEliminar());
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> ocultarDialogos());

        // Navegación barra inferior
        v.findViewById(R.id.house).setOnClickListener(this);
        v.findViewById(R.id.candadoclose).setOnClickListener(this);
        v.findViewById(R.id.carpeta).setOnClickListener(this);
        v.findViewById(R.id.archivo).setOnClickListener(this);
        v.findViewById(R.id.mail).setOnClickListener(this);
        v.findViewById(R.id.btnperfil).setOnClickListener(this);

        cargarDatos();
    }

    private void cargarDatos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> descifrados = archivoDAO.getAllDescifrados();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    listaMetadata.clear();
                    listaMetadata.addAll(descifrados);
                    adaptador.notifyDataSetChanged();
                });
            }
        });
    }

    private void ocultarDialogos() {
        dialogContainerCifrar.setVisibility(View.GONE);
        dialogContainerEliminar.setVisibility(View.GONE);
        posicionSeleccionada = -1;
    }

    @Override
    public void onDescifrarClick(int position) {
        this.posicionSeleccionada = position;
        dialogContainerCifrar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBorrarClick(int position) {
        this.posicionSeleccionada = position;
        dialogContainerEliminar.setVisibility(View.VISIBLE);
    }

    // --- LOGICA DE RE-CIFRADO CON BORRADO TOTAL ---
    private void ejecutarAccionCifrar() {
        if (posicionSeleccionada == -1) return;

        ArchivoMetadata archivo = listaMetadata.get(posicionSeleccionada);
        ocultarDialogos();

        Executors.newSingleThreadExecutor().execute(() -> {
            // 1. ELIMINACIÓN FÍSICA DEL ARCHIVO DESCIFRADO
            if (archivo.getRutaLocalDescifrado() != null) {
                File f = new File(archivo.getRutaLocalDescifrado());
                if (f.exists()) {
                    boolean borrado = f.delete();
                    // Notificar al sistema para limpiar rastros en galería/Files
                    if (borrado) {
                        try {
                            requireContext().getContentResolver().delete(
                                    Uri.fromFile(f), null, null);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 2. ACTUALIZACIÓN DE BASE DE DATOS
            archivo.setEstaCifrado(true);
            archivo.setRutaLocalDescifrado(null); // Eliminamos la ruta para que no existan huellas
            archivoDAO.update(archivo);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Archivo original destruido. Re-cifrando...", Toast.LENGTH_SHORT).show();

                    // Definir el destino final según el origen
                    int destinoFinalId = "ESCANEO".equals(archivo.getOrigen())
                            ? R.id.cifradoEscaneo2
                            : R.id.archivosCifrados2;

                    Bundle bundle = new Bundle();
                    bundle.putInt("destino_final", destinoFinalId);

                    Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, bundle);
                });
            }
        });
    }

    private void ejecutarAccionEliminar() {
        if (posicionSeleccionada == -1) return;
        ArchivoMetadata archivo = listaMetadata.get(posicionSeleccionada);
        ocultarDialogos();

        Executors.newSingleThreadExecutor().execute(() -> {
            // Borrado físico
            if (archivo.getRutaLocalDescifrado() != null) {
                File f = new File(archivo.getRutaLocalDescifrado());
                if (f.exists()) {
                    f.delete();
                    try {
                        requireContext().getContentResolver().delete(
                                Uri.fromFile(f), null, null);
                    } catch (Exception ignored) {}
                }
            }

            // Borrado de base de datos
            archivoDAO.delete(archivo);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    listaMetadata.remove(archivo);
                    adaptador.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Eliminado permanentemente", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        ArchivoMetadata archivo = listaMetadata.get(position);
        if (archivo.getRutaLocalDescifrado() == null) {
            Toast.makeText(getContext(), "El archivo ya no existe localmente", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(archivo.getRutaLocalDescifrado());
        if (!file.exists()) {
            Toast.makeText(getContext(), "Archivo no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
            String extension = MimeTypeMap.getFileExtensionFromUrl(contentUri.toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType != null ? mimeType : "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Abrir con:"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al abrir", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.house) Navigation.findNavController(v).navigate(R.id.inicio);
        else if (id == R.id.candadoclose) Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        else if (id == R.id.carpeta) Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.archivo) Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        else if (id == R.id.mail) Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        else if (id == R.id.btnperfil) Navigation.findNavController(v).navigate(R.id.perfil2);
    }

    @Override public void onCambiarEstadoClick(int position) {}
}