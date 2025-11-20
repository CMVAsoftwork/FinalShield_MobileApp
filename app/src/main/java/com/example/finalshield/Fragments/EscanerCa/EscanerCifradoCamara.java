package com.example.finalshield.Fragments.EscanerCa;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    // Prefijo para identificar las fotos de esta sesión en la caché
    private static final String FOTO_PREFIX = "CAMARA_TEMP_";

    ImageButton addele1, recortar1, edicion1, eliminar1;
    Button regresar, guardarPdf;
    private PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Button tomarfoto;
    private ImageView imagencita;
    private TextView contadorfot;
    private int contador = 0;
    private List<File> fotosTomadas = new ArrayList<>();
    private static final int CAMERA_REQUEST_CODE = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_camara, container, false);
    }

    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        vistaPrevia = v.findViewById(R.id.vistaprevia);

        regresar = v.findViewById(R.id.regresar4);
        guardarPdf = v.findViewById(R.id.guardar);

        tomarfoto = v.findViewById(R.id.tomarfoto);
        imagencita = v.findViewById(R.id.imagencita);
        contadorfot = v.findViewById(R.id.Contadorfot);

        addele1 = v.findViewById(R.id.addelements1);
        recortar1 = v.findViewById(R.id.recortar1);
        edicion1 = v.findViewById(R.id.edicion1);
        eliminar1 = v.findViewById(R.id.eliminar1);

        tomarfoto.setOnClickListener(this);

        if (imagencita != null) imagencita.setOnClickListener(this);
        addele1.setOnClickListener(this);
        recortar1.setOnClickListener(this);
        edicion1.setOnClickListener(this);
        eliminar1.setOnClickListener(this);
        regresar.setOnClickListener(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    private void limpiarArchivosDeSesionAnterior() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith(FOTO_PREFIX) && name.endsWith(".jpg"));

        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    Log.d("CamaraCache", "Archivo de sesión anterior eliminado: " + file.getName());
                } else {
                    Log.e("CamaraCache", "No se pudo eliminar el archivo: " + file.getName());
                }
            }
        }
        contador = 0;
        fotosTomadas.clear();
        if (contadorfot != null) actualizarContadorUI();
        if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
    }

    // se llama al regresar de VerFotosTomadas para sincronizar
    @Override
    public void onResume() {
        super.onResume();
        // esto re-leerá el disco y contará las fotos que quedaron, las no borradas.
        recontarFotosDesdeCache();
    }

    private void recontarFotosDesdeCache() {
        fotosTomadas.clear(); // se limpia la lista en memoria
        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                // solo se cuentan los que aún existen Y tienen el prefijo (no descartados)
                if (file.isFile() && file.getName().startsWith(FOTO_PREFIX) && file.getName().endsWith(".jpg")) {
                    fotosTomadas.add(file);
                }
            }
        }

        contador = fotosTomadas.size();
        actualizarContadorUI();
        // actualiza la miniatura
        if (!fotosTomadas.isEmpty()) {
            mostrarUltimaFoto(fotosTomadas.get(fotosTomadas.size() - 1));
        } else {
            // borra la imagen con Glide
            Glide.with(this).load((String) null).into(imagencita);
        }
    }

    private void actualizarContadorUI() {
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
                // navegar al destino
                Navigation.findNavController(v).navigate(R.id.escanerCaVerFotosTomadas, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para visualizar.", Toast.LENGTH_SHORT).show();
            }
        }else if(v.getId() == R.id.regresar4){
            // si el usuario presiona regresar, cancela la sesión y borramos los archivos.
            limpiarArchivosDeSesionAnterior();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (v.getId() == R.id.tomarfoto) {
            tomarFoto();
        }else if (v.getId() == R.id.addelements1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaEscanearMasPaginas);
        } else if (v.getId() == R.id.recortar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaCortarRotar);
        } else if (v.getId() == R.id.edicion1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaVisualizacionYReordenamiento);
        } else if (v.getId() == R.id.eliminar1) {
            Navigation.findNavController(v).navigate(R.id.escanerCaEliminarPaginas);
        }
    }

    private void tomarFoto() {
        if (imageCapture == null) {
            Log.e("EscanerCifrado", "ImageCapture no está inicializado.");
            Toast.makeText(requireContext(), "Cámara no lista.", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDirectory = requireContext().getCacheDir();
        //usar el prefijo en el nombre del archivo
        File photoFile = new File(outputDirectory, FOTO_PREFIX + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        fotosTomadas.add(photoFile);
                        contador = fotosTomadas.size();
                        actualizarContadorUI();
                        mostrarUltimaFoto(photoFile);
                        Toast.makeText(requireContext(), "Foto #" + contador + " capturada.", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("EscanerCifrado", "Error al capturar la imagen: " + exception.getMessage(), exception);
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