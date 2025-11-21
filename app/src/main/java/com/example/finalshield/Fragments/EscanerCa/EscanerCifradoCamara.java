package com.example.finalshield.Fragments.EscanerCa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.finalshield.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.example.finalshield.R; // Asegúrate de que esta importación sea correcta

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EscanerCifradoCamara extends Fragment implements View.OnClickListener {

    private static final String FOTO_PREFIX = "CAMARA_TEMP_";
    private static final int CAMERA_REQUEST_CODE = 10;

    // Vistas y CameraX
    ImageButton recortar1, eliminar1;
    Button regresar, guardarPdf;
    private PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Button tomarfoto;
    private ImageView imagencita;
    private TextView contadorfot;

    // Datos de sesión
    private int contador = 0;
    private List<File> fotosTomadas = new ArrayList<>();

    // Claves del resultado de reordenamiento/eliminación
    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_camara, container, false);
    }

    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // **1. Manejo del Resultado de Reordenamiento/Eliminación (Solo actualización de datos)**
        getParentFragmentManager().setFragmentResultListener(
                KEY_REORDENAR_RESULT,
                this,
                (requestKey, result) -> {
                    if (requestKey.equals(KEY_REORDENAR_RESULT)) {

                        ArrayList<String> nuevasRutasStr = result.getStringArrayList(BUNDLE_REORDENAR_URI_LIST);

                        if (nuevasRutasStr != null && !nuevasRutasStr.isEmpty()) {

                            // Reconstruir la lista 'fotosTomadas' en el nuevo orden
                            List<File> reordenadas = new ArrayList<>();
                            for (String uriStr : nuevasRutasStr) {
                                File file = getFileFromUri(Uri.parse(uriStr));
                                if (file != null && file.exists()) {
                                    reordenadas.add(file);
                                }
                            }

                            // Actualizar la lista principal
                            fotosTomadas.clear();
                            fotosTomadas.addAll(reordenadas);

                            // *** NOTA: No actualizamos la UI aquí, lo hará onResume() ***

                        } else {
                            // Lista vacía (todos los archivos fueron eliminados)
                            limpiarArchivosDeSesionAnterior();
                        }
                    }
                });

        // 2. Inicialización de Vistas
        vistaPrevia = v.findViewById(R.id.vistaprevia);
        regresar = v.findViewById(R.id.regresar4);
        guardarPdf = v.findViewById(R.id.guardar);
        tomarfoto = v.findViewById(R.id.tomarfoto);
        imagencita = v.findViewById(R.id.imagencita);
        contadorfot = v.findViewById(R.id.Contadorfot);
        recortar1 = v.findViewById(R.id.recortar1);
        eliminar1 = v.findViewById(R.id.eliminar1);

        // 3. Listeners
        tomarfoto.setOnClickListener(this);
        if (imagencita != null) imagencita.setOnClickListener(this);
        recortar1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);
        regresar.setOnClickListener(this);

        // 4. Iniciar Cámara
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    /**
     * Resuelve un Uri de FileProvider a un objeto File buscando en la caché.
     */
    private File getFileFromUri(Uri uri) {
        if (uri.getLastPathSegment() == null) return null;

        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();

        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                // Heurística: compara si el nombre del archivo contiene el segmento final del URI.
                if (file.getName().contains(uri.getLastPathSegment())) {
                    return file;
                }
            }
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // CORRECCIÓN CLAVE: Llama a actualizar la UI siempre que el fragmento esté visible
        // Si fotosTomadas está vacío, llama a recontar (que ya actualiza la UI).
        if (fotosTomadas.isEmpty()) {
            recontarFotosDesdeCache();
        } else {
            // Si la lista tiene datos (ya sea recién tomada o regresando de otra pantalla),
            // asegura que la UI refleje el estado actual de esa lista.
            actualizarUIConDatosActuales();
        }
    }

    /**
     * Actualiza el contador y la miniatura con la última foto de la lista actual.
     */
    private void actualizarUIConDatosActuales() {
        if (!fotosTomadas.isEmpty()) {
            // Actualizar contador
            contador = fotosTomadas.size();
            actualizarContadorUI();

            // Mostrar la última foto de la lista (ya reordenada o eliminada)
            mostrarUltimaFoto(fotosTomadas.get(fotosTomadas.size() - 1));
        } else {
            // Si está vacía después de una eliminación, limpia la UI
            limpiarArchivosDeSesionAnterior();
        }
    }

    private void limpiarArchivosDeSesionAnterior() {
        // Elimina archivos temporales de caché
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith(FOTO_PREFIX) && name.endsWith(".jpg"));

        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        contador = 0;
        fotosTomadas.clear();
        if (contadorfot != null) actualizarContadorUI();
        if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
    }

    private void recontarFotosDesdeCache() {
        fotosTomadas.clear();
        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isFile() && file.getName().startsWith(FOTO_PREFIX) && file.getName().endsWith(".jpg")) {
                    fotosTomadas.add(file);
                }
            }
        }

        // Después de recontar, actualiza la UI
        actualizarUIConDatosActuales();
    }

    private void actualizarContadorUI() {
        // Solo actualiza el TextView, ya no calcula el contador
        contadorfot.setText(String.valueOf(contador));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.imagencita) {
            if (!fotosTomadas.isEmpty()) {
                Bundle bundle = new Bundle();
                ArrayList<String> filePaths = new ArrayList<>();
                for (File file : fotosTomadas) {
                    filePaths.add(file.getAbsolutePath());
                }
                bundle.putStringArrayList("FOTOS_CAPTURA", filePaths);
                Navigation.findNavController(v).navigate(R.id.escanerCaReordenar, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para visualizar.", Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.regresar4){
            limpiarArchivosDeSesionAnterior();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (id == R.id.tomarfoto) {
            tomarFoto();
        } else if (id == R.id.recortar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaCortarRotar);
        } else if (id == R.id.eliminar1) {
            if (!fotosTomadas.isEmpty()) {
                Bundle bundle = new Bundle();
                ArrayList<String> filePaths = new ArrayList<>();
                for (File file : fotosTomadas) {
                    filePaths.add(file.getAbsolutePath());
                }
                bundle.putStringArrayList("FOTOS_CAPTURA", filePaths);
                Navigation.findNavController(v).navigate(R.id.escanerCaVerFotosTomadas, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para visualizar/eliminar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void tomarFoto() {
        if (imageCapture == null) return;

        File outputDirectory = requireContext().getCacheDir();
        File photoFile = new File(outputDirectory, FOTO_PREFIX + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        fotosTomadas.add(photoFile);

                        // Actualiza la UI inmediatamente después de tomar la foto
                        actualizarUIConDatosActuales();

                        Toast.makeText(requireContext(), "Foto #" + contador + " capturada.", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("EscanerCifrado", "Error al capturar la imagen: " + exception.getMessage());
                        Toast.makeText(requireContext(), "Error al tomar foto.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void mostrarUltimaFoto(File photoFile) {
        Glide.with(this)
                .load(photoFile)
                .centerCrop()
                .into(imagencita);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara no concedido.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                imageCapture = new ImageCapture.Builder().build();
                preview.setSurfaceProvider(vistaPrevia.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("EscanerCifrado", "Error al inicializar CameraX.", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}