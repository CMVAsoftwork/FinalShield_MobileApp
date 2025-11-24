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
import androidx.core.content.FileProvider;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.finalshield.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
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

    ImageButton recortar1, eliminar1;
    Button regresar, guardarPdf;
    private androidx.camera.view.PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Button tomarfoto;
    private ImageView imagencita;
    private TextView contadorfot;

    private int contador = 0;
    private final List<File> fotosTomadas = new ArrayList<>();

    public static final String KEY_REORDENAR_RESULT = "reordenar_key_verfotos";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_camara, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Escucha los resultados que vienen de Reordenar / VerFotos / CortarRotar
        getParentFragmentManager().setFragmentResultListener(KEY_REORDENAR_RESULT, this, (requestKey, result) -> {
            if (requestKey.equals(KEY_REORDENAR_RESULT)) {
                ArrayList<String> nuevasRutasStr = result.getStringArrayList(BUNDLE_REORDENAR_URI_LIST);
                if (nuevasRutasStr != null) {
                    List<File> reordenadas = new ArrayList<>();
                    for (String uriStr : nuevasRutasStr) {
                        try {
                            Uri u = Uri.parse(uriStr);
                            File f = getFileFromUri(u);
                            if (f != null && f.exists()) reordenadas.add(f);
                            else Log.w("Camara", "Archivo no encontrado para URI devuelta: " + uriStr);
                        } catch (Exception ex) {
                            Log.e("Camara", "Error parseando URI devuelta: " + uriStr, ex);
                        }
                    }

                    fotosTomadas.clear();
                    fotosTomadas.addAll(reordenadas);
                    actualizarUIConDatosActuales();
                }
            }
        });

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

        if (allPermissionsGranted()) startCamera();
        else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    /**
     * Resuelve un Uri (FileProvider) a File buscando en cache dir por nombre.
     */
    private File getFileFromUri(Uri uri) {
        if (uri == null) return null;
        String fileName = uri.getLastPathSegment();
        if (fileName == null) return null;

        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isFile() && file.getName().equals(fileName)) return file;
            }
        }
        return null;
    }

    private ArrayList<String> crearListaUrisParaNavegacion() {
        ArrayList<String> uriStrings = new ArrayList<>();
        for (File file : fotosTomadas) {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            uriStrings.add(fileUri.toString());
        }
        return uriStrings;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fotosTomadas.isEmpty()) recontarFotosDesdeCache();
        else actualizarUIConDatosActuales();
    }

    private void actualizarUIConDatosActuales() {
        if (!fotosTomadas.isEmpty()) {
            contador = fotosTomadas.size();
            actualizarContadorUI();
            mostrarUltimaFoto(fotosTomadas.get(fotosTomadas.size() - 1));
        } else {
            limpiarUI();
        }
    }

    private void limpiarUI() {
        contador = 0;
        if (contadorfot != null) actualizarContadorUI();
        if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
    }

    private void limpiarArchivosDeSesionAnterior() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) ->
                (name.startsWith(FOTO_PREFIX) || name.startsWith("EDITADO_")) && name.endsWith(".jpg"));

        if (files != null) {
            for (File file : files) file.delete();
        }
        fotosTomadas.clear();
        limpiarUI();
    }

    private void recontarFotosDesdeCache() {
        fotosTomadas.clear();
        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();

        List<File> tempFiles = new ArrayList<>();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isFile() && (file.getName().startsWith(FOTO_PREFIX) || file.getName().startsWith("EDITADO_")) && file.getName().endsWith(".jpg")) {
                    tempFiles.add(file);
                }
            }
        }

        tempFiles.sort(Comparator.comparingLong(File::lastModified));
        fotosTomadas.addAll(tempFiles);
        actualizarUIConDatosActuales();
    }

    private void actualizarContadorUI() {
        contadorfot.setText(String.valueOf(contador));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imagencita) {
            if (!fotosTomadas.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("FOTOS_CAPTURA", crearListaUrisParaNavegacion());
                Navigation.findNavController(v).navigate(R.id.escanerCaReordenar, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para visualizar.", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.regresar4) {
            limpiarArchivosDeSesionAnterior();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (id == R.id.tomarfoto) {
            tomarFoto();
        } else if (id == R.id.recortar1) {
            if (!fotosTomadas.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("FOTOS_CAPTURA", crearListaUrisParaNavegacion());
                Navigation.findNavController(v).navigate(R.id.escanerCaCortarRotar, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para editar.", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.eliminar1) {
            if (!fotosTomadas.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("FOTOS_CAPTURA", crearListaUrisParaNavegacion());
                Navigation.findNavController(v).navigate(R.id.escanerCaVerFotosTomadas, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para eliminar.", Toast.LENGTH_SHORT).show();
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
                        actualizarUIConDatosActuales();
                        Toast.makeText(requireContext(), "Foto capturada.", Toast.LENGTH_SHORT).show();
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
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .into(imagencita);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (allPermissionsGranted()) startCamera();
            else Toast.makeText(requireContext(), "Permiso de cámara no concedido.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                androidx.camera.core.CameraSelector cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA;
                imageCapture = new ImageCapture.Builder().build();
                preview.setSurfaceProvider(vistaPrevia.getSurfaceProvider());
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Log.e("EscanerCifrado", "Error al inicializar CameraX.", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }
}