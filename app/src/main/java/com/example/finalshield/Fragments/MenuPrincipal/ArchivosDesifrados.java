package com.example.finalshield.Fragments.MenuPrincipal;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_desifrados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        listView = v.findViewById(R.id.listadesc);
        adaptador = new AdaptadorArchivos(getContext(), listaMetadata, this);
        listView.setAdapter(adaptador);

        // Listeners de navegación originales
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

    @Override
    public void onItemClick(int position) {
        ArchivoMetadata archivo = listaMetadata.get(position);
        if (archivo.getRutaLocalDescifrado() == null) return;

        File file = new File(archivo.getRutaLocalDescifrado());
        if (!file.exists()) {
            Toast.makeText(getContext(), "El archivo no existe", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            // Obtener MimeType real basado en la extensión física del archivo
            String extension = MimeTypeMap.getFileExtensionFromUrl(contentUri.toString());
            if (extension == null || extension.isEmpty()) {
                extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
            }

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mimeType == null) mimeType = "*/*"; // Genérico si no se reconoce

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(Intent.createChooser(intent, "Abrir archivo con:"));

        } catch (Exception e) {
            Toast.makeText(getContext(), "No hay aplicaciones para abrir este archivo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDescifrarClick(int position) {
        // ACCIÓN: RE-CIFRAR (Regresar a la bóveda)
        ArchivoMetadata archivo = listaMetadata.get(position);
        archivo.setEstaCifrado(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            archivoDAO.update(archivo);

            // Borrar el archivo físico para no dejar rastro (Seguridad)
            if (archivo.getRutaLocalDescifrado() != null) {
                File f = new File(archivo.getRutaLocalDescifrado());
                if (f.exists()) f.delete();
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Regresado a la bóveda", Toast.LENGTH_SHORT).show();

                    // Navegar según el origen
                    if ("ESCANEO".equals(archivo.getOrigen())) {
                        Navigation.findNavController(requireView()).navigate(R.id.cifradoEscaneo2);
                    } else {
                        Navigation.findNavController(requireView()).navigate(R.id.archivosCifrados2);
                    }
                });
            }
        });
    }

    @Override
    public void onBorrarClick(int position) {
        ArchivoMetadata archivo = listaMetadata.get(position);
        Executors.newSingleThreadExecutor().execute(() -> {
            archivoDAO.delete(archivo);
            if (archivo.getRutaLocalDescifrado() != null) {
                File f = new File(archivo.getRutaLocalDescifrado());
                if (f.exists()) f.delete();
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    listaMetadata.remove(position);
                    adaptador.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
                });
            }
        });
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