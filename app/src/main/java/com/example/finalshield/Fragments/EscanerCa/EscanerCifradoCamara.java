package com.example.finalshield.Fragments.EscanerCa;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Fragments.Escaner.EscanerProcesador;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EscanerCifradoCamara extends Fragment implements View.OnClickListener {

    private static final String FOTO_PREFIX = "CAMARA_TEMP_";

    // ✅ 1. REGISTRO DE PERMISOS (Activa la cámara inmediatamente al aceptar)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera(); // Se ejecuta en el momento que el usuario pulsa "Permitir"
                } else {
                    Toast.makeText(requireContext(), "Se requiere permiso de cámara", Toast.LENGTH_SHORT).show();
                }
            });

    ImageButton recortar1, eliminar1;
    Button regresar, guardarPdf;
    private PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Button tomarfoto;
    private ImageView imagencita;
    private TextView contadorfot;

    private int contador = 0;
    private final List<File> fotosTomadas = new ArrayList<>();

    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_camara, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        vistaPrevia = v.findViewById(R.id.vistaprevia);
        regresar = v.findViewById(R.id.regresar4);
        guardarPdf = v.findViewById(R.id.guardar);
        tomarfoto = v.findViewById(R.id.tomarfoto);
        imagencita = v.findViewById(R.id.imagencita);
        contadorfot = v.findViewById(R.id.Contadorfot);
        recortar1 = v.findViewById(R.id.recortar1);
        eliminar1 = v.findViewById(R.id.eliminar1);

        tomarfoto.setOnClickListener(this);
        if (imagencita != null) imagencita.setOnClickListener(this);
        recortar1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardarPdf.setOnClickListener(this);

        // ✅ 2. VERIFICACIÓN DE PERMISOS INICIAL
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.tomarfoto) {
            tomarFoto();
        } else if (id == R.id.guardar) {
            if (!fotosTomadas.isEmpty()) {
                v.setEnabled(false); // Evitar doble clic
                final androidx.navigation.NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.cargaProcesos);

                List<Uri> urisParaCifrar = new ArrayList<>();
                for (File f : fotosTomadas) {
                    urisParaCifrar.add(FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider", f));
                }

                // 3. PROCESO DE CIFRADO Y BORRADO TOTAL
                EscanerProcesador.generarPdfYEnviar(requireContext(), urisParaCifrar, archivoService, archivoDAO, () -> {

                    // ✅ BORRADO DE ARCHIVOS TRAS CIFRADO EXITOSO
                    borrarOriginalesExhaustivo(urisParaCifrar);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        limpiarArchivosDeSesionAnterior();
                        navController.navigate(R.id.cifradoEscaneo2);
                        Toast.makeText(getContext(), "Archivos protegidos y originales eliminados", Toast.LENGTH_SHORT).show();
                    });
                });
            }
        } else if (id == R.id.regresar4) {
            limpiarArchivosDeSesionAnterior();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        }
        // ... (resto de tus IDs se mantienen igual)
    }

    // ✅ 4. MÉTODO DE BORRADO PARA LIMPIAR RASTROS
    private void borrarOriginalesExhaustivo(List<Uri> uris) {
        ContentResolver resolver = requireContext().getContentResolver();
        for (Uri uri : uris) {
            try {
                // Borrar del MediaStore si existe rastro en galería
                resolver.delete(uri, null, null);

                // Borrar físicamente si es archivo de caché
                File f = getFileFromUri(uri);
                if (f != null && f.exists()) f.delete();

            } catch (Exception e) {
                Log.e("Borrado", "No se pudo eliminar el original: " + uri);
            }
        }
        // Notificar al sistema para refrescar miniaturas de galería
        if (!uris.isEmpty()) {
            requireContext().sendBroadcast(new android.content.Intent(
                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uris.get(0)));
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                preview.setSurfaceProvider(vistaPrevia.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (Exception e) {
                Log.e("Camara", "Error CameraX", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void tomarFoto() {
        if (imageCapture == null) return;
        File photoFile = new File(requireContext().getCacheDir(), FOTO_PREFIX + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                fotosTomadas.add(photoFile);
                actualizarUIConDatosActuales();
            }
            @Override
            public void onError(@NonNull ImageCaptureException e) {
                Toast.makeText(requireContext(), "Error al capturar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- MANTENIMIENTO DE TUS MÉTODOS DE UI ---

    private void limpiarArchivosDeSesionAnterior() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) ->
                (name.startsWith(FOTO_PREFIX) || name.startsWith("EDITADO_")) && name.endsWith(".jpg"));
        if (files != null) for (File file : files) file.delete();
        fotosTomadas.clear();
        limpiarUI();
    }

    private void actualizarUIConDatosActuales() {
        if (!fotosTomadas.isEmpty()) {
            contador = fotosTomadas.size();
            if (contadorfot != null) contadorfot.setText(String.valueOf(contador));
            Glide.with(this).load(fotosTomadas.get(fotosTomadas.size() - 1))
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).centerCrop().into(imagencita);
        } else {
            limpiarUI();
        }
    }

    private void limpiarUI() {
        contador = 0;
        if (contadorfot != null) contadorfot.setText("0");
        if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
    }

    private File getFileFromUri(Uri uri) {
        if (uri == null) return null;
        String fileName = uri.getLastPathSegment();
        File file = new File(requireContext().getCacheDir(), fileName);
        return file.exists() ? file : null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fotosTomadas.isEmpty()) recontarFotosDesdeCache();
        else actualizarUIConDatosActuales();
    }

    private void recontarFotosDesdeCache() {
        fotosTomadas.clear();
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) ->
                (name.startsWith(FOTO_PREFIX) || name.startsWith("EDITADO_")) && name.endsWith(".jpg"));
        if (files != null) {
            List<File> temp = new ArrayList<>(List.of(files));
            temp.sort(Comparator.comparingLong(File::lastModified));
            fotosTomadas.addAll(temp);
        }
        actualizarUIConDatosActuales();
    }
}