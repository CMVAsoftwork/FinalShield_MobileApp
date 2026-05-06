package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Util.SecurityUtils;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ArchivosDesifrados extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private ListView listView;
    private AdaptadorArchivos adaptador;
    private final List<ArchivoMetadata> listaMetadata = new ArrayList<>();
    private ArchivoDAO archivoDAO;
    private CargaViewModel cargaViewModel;

    private LinearLayout dialogContainerCifrar, dialogContainerEliminar;
    private int posicionSeleccionada = -1;
    private int intentosHuella = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Usamos el Activity para que el ViewModel persista entre fragmentos
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.inicio);
            }
        });
    }

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

        v.findViewById(R.id.sicifrar).setOnClickListener(view -> ejecutarAccionCifrar());
        v.findViewById(R.id.nocifrar).setOnClickListener(view -> dialogContainerCifrar.setVisibility(View.GONE));

        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> solicitarHuellaParaEliminar());
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> dialogContainerEliminar.setVisibility(View.GONE));

        int[] navIds = {R.id.house, R.id.candadoclose, R.id.carpeta, R.id.archivo, R.id.mail, R.id.btnperfil, R.id.candadopen};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        cargarDatos();
    }

    private void cargarDatos() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ArchivoMetadata> descifrados = archivoDAO.getAllDescifrados();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    listaMetadata.clear();
                    listaMetadata.addAll(descifrados);
                    adaptador.notifyDataSetChanged();
                }
            });
        });
    }

    private void ejecutarAccionCifrar() {
        if (posicionSeleccionada < 0 || posicionSeleccionada >= listaMetadata.size()) return;

        ArchivoMetadata archivo = listaMetadata.get(posicionSeleccionada);
        dialogContainerCifrar.setVisibility(View.GONE);

        // 1. Limpieza total previa del estado
        cargaViewModel.resetear();

        // 2. Preparar el destino
        int destinoFinalId = "ESCANEO".equals(archivo.getOrigen()) ? R.id.cifradoEscaneo2 : R.id.archivosCifrados2;
        Bundle args = new Bundle();
        args.putInt("destino_final", destinoFinalId);

        // 3. NAVEGAR PRIMERO: Esto asegura que el Fragment de carga tome el control de la UI
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);

        // 4. Ejecutar la chamba pesada con un margen de seguridad
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Este delay es vital: le da tiempo al NavController de hacer la transición
                // y al CargaProcesosFragment de registrar su Observer en el ViewModel
                Thread.sleep(1000);

                // Lógica de seguridad
                if (archivo.getRutaLocalDescifrado() != null) {
                    SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
                }

                archivo.setEstaCifrado(true);
                archivo.setRutaLocalDescifrado(null);
                archivoDAO.update(archivo);

                // 5. Notificar el fin. Usamos postDelayed para garantizar que el LiveData
                // cambie cuando la UI ya esté estable.
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d("FinalShield", "Proceso terminado, disparando navegación final...");
                    cargaViewModel.terminarProceso();
                    posicionSeleccionada = -1;
                }, 500);

            } catch (Exception e) {
                Log.e("FinalShield", "Error en re-cifrado", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    cargaViewModel.resetear();
                    if (isAdded()) {
                        Navigation.findNavController(requireView()).popBackStack();
                    }
                });
            }
        });
    }

    private void solicitarHuellaParaEliminar() {
        dialogContainerEliminar.setVisibility(View.GONE);
        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                ejecutarEliminacionFisica();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("FinalShield - Triturado")
                .setNegativeButtonText("Cancelar")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void ejecutarEliminacionFisica() {
        if (posicionSeleccionada < 0 || posicionSeleccionada >= listaMetadata.size()) return;
        final ArchivoMetadata archivo = listaMetadata.get(posicionSeleccionada);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (archivo.getRutaLocalDescifrado() != null) {
                    SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
                }
                archivoDAO.delete(archivo);
                new Handler(Looper.getMainLooper()).post(this::cargarDatos);
            } catch (Exception e) {
                Log.e("FinalShield", "Error borrado", e);
            }
        });
    }

    @Override
    public void onItemClick(int position) {
        if (!isAdded() || position < 0 || position >= listaMetadata.size()) return;
        ArchivoMetadata archivo = listaMetadata.get(position);
        if (archivo.getRutaLocalDescifrado() == null) return;
        File file = new File(archivo.getRutaLocalDescifrado());

        try {
            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType != null ? mimeType : "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Abrir con:"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al abrir", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onDescifrarClick(int p) { posicionSeleccionada = p; dialogContainerCifrar.setVisibility(View.VISIBLE); }
    @Override public void onBorrarClick(int p) { posicionSeleccionada = p; dialogContainerEliminar.setVisibility(View.VISIBLE); }
    @Override public void onCambiarEstadoClick(int pos) {}

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);
        if (id == R.id.house) nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        else if (id == R.id.candadoclose) nav.navigate(R.id.archivosCifrados2);
        else if (id == R.id.carpeta) nav.navigate(R.id.cifradoEscaneo2);
        else if (id == R.id.archivo) nav.navigate(R.id.archivosCifrados);
        else if (id == R.id.mail) nav.navigate(R.id.servivioCorreo);
        else if (id == R.id.btnperfil) nav.navigate(R.id.perfil2);
        else if (id == R.id.candadopen) cargarDatos();
    }
}