package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.pm.PackageManager;
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
import androidx.lifecycle.ViewModelProvider;
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
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EscanerCifradoMixto extends Fragment implements View.OnClickListener {

    private static final String FOTO_PREFIX = "CAMARA_TEMP_";
    private static final int CAMERA_REQUEST_CODE = 10;

    ImageButton recortar, eliminar, selectimg,acomodar;
    Button regresar, guardarPdf;
    private androidx.camera.view.PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Button tomarfoto;
    private ImageView imagencita;
    private TextView contadorfot;

    private int contador = 0;
    private SharedImageViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Asegúrate de reemplazar "R.layout.fragment_escaner_cifrado_mixto" con el layout correcto.
        return inflater.inflate(R.layout.fragment_escaner_cifrado_mixto, container, false);
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
        selectimg = v.findViewById(R.id.selecgaleria);
        acomodar = v.findViewById(R.id.edicion);
        recortar = v.findViewById(R.id.recortar);
        eliminar = v.findViewById(R.id.eliminar);

        tomarfoto.setOnClickListener(this);
        if (imagencita != null) imagencita.setOnClickListener(this);
        recortar.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regresar.setOnClickListener(this);
        selectimg.setOnClickListener(this);
        acomodar.setOnClickListener(this);

        if (allPermissionsGranted()) startCamera();
        else requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);

        recontarFotosDesdeCache();
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarUIConDatosActuales();
    }

    // --- LÓGICA DE SINCRONIZACIÓN Y UI ---

    private void actualizarUIConDatosActuales() {
        // La lista combinada es necesaria para comprobar si hay elementos guardados en general.
        List<Uri> listaCombinada = sharedViewModel.getImageUriList();

        // La lista de SOLO CÁMARA es necesaria para la miniatura y ahora, el contador.
        List<Uri> listaCamara = sharedViewModel.getCameraOnlyUriList();

        // 🌟 CAMBIO CLAVE: El contador ahora solo refleja los elementos de la cámara
        contador = listaCamara.size();
        actualizarContadorUI();

        if (!listaCamara.isEmpty()) {
            // Se muestra la última URI de la lista de SOLO CÁMARA
            Uri lastCameraUri = listaCamara.get(listaCamara.size() - 1);
            File lastCameraFile = getFileFromUri(lastCameraUri);

            if (lastCameraFile != null && lastCameraFile.exists()) {
                mostrarUltimaFoto(lastCameraFile);
            } else {
                Log.w("Camara", "Archivo de la última foto de cámara no encontrado. Limpiando miniatura.");
                if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
            }
        } else if (!listaCombinada.isEmpty()) {
            // Si no hay fotos de CÁMARA pero sí de GALERÍA, limpiamos la miniatura.
            if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
        } else {
            // Si ambas listas están vacías
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
                (name.startsWith(FOTO_PREFIX) || name.startsWith("EDITADO_") || name.startsWith("editable_")) && name.endsWith(".jpg"));

        if (files != null) {
            for (File file : files) file.delete();
        }
        sharedViewModel.clearList();
        sharedViewModel.clearCameraOnlyList();
        limpiarUI();
    }

    private void recontarFotosDesdeCache() {
        List<Uri> tempUris = new ArrayList<>();
        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();

        List<File> tempFiles = new ArrayList<>();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isFile() && (file.getName().startsWith(FOTO_PREFIX) || file.getName().startsWith("EDITADO_") || file.getName().startsWith("editable_")) && file.getName().endsWith(".jpg")) {
                    tempFiles.add(file);
                }
            }
        }

        tempFiles.sort(Comparator.comparingLong(File::lastModified));
        for (File file : tempFiles) {
            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            tempUris.add(fileUri);
        }

        if (!tempUris.isEmpty()) {
            sharedViewModel.setImageUriList(tempUris);

            // Inicializar la lista de la cámara correctamente
            List<Uri> camaraUris = new ArrayList<>();
            for (File file : tempFiles) {
                if (file.getName().startsWith(FOTO_PREFIX)) {
                    camaraUris.add(FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file));
                }
            }
            sharedViewModel.setCameraOnlyUriList(camaraUris);
        }
        actualizarUIConDatosActuales();
    }

    private void actualizarContadorUI() {
        contadorfot.setText(String.valueOf(contador));
    }

    private File getFileFromUri(Uri uri) {
        if (uri == null) return null;

        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            String fileName = uri.getLastPathSegment();
            if (fileName == null) return null;

            File cacheDir = requireContext().getCacheDir();
            File file = new File(cacheDir, fileName);
            if (file.exists()) return file;
        }
        else if (uri.getScheme() != null && uri.getScheme().equals("file")) {
            return new File(uri.getPath());
        }
        return null;
    }

    // --- LÓGICA DE CLICK Y NAVEGACIÓN ---

    @Override
    public void onClick(View v) {
        int id = v.getId();
        List<Uri> listaActual = sharedViewModel.getImageUriList();

        if(id == R.id.imagencita) {
            // Navegar a la vista de reordenamiento de SOLO CÁMARA
            if (!sharedViewModel.getCameraOnlyUriList().isEmpty()) {
                Navigation.findNavController(v).navigate(R.id.escanerReordenarFotosTomadas);
            } else {
                Toast.makeText(requireContext(), "No hay fotos de cámara para visualizar/reordenar.", Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.regresar4){
            limpiarArchivosDeSesionAnterior();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (id == R.id.tomarfoto) {
            tomarFoto();
        } else if (id == R.id.recortar) {
            if (listaActual.isEmpty()) {
                Toast.makeText(requireContext(), "No hay fotos para recortar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        } else if (id == R.id.edicion) {
            if (listaActual.isEmpty()) {
                Toast.makeText(requireContext(), "No hay elementos para acomodar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (id == R.id.eliminar) {
            if (listaActual.isEmpty()) {
                Toast.makeText(requireContext(), "No hay fotos para eliminar.", Toast.LENGTH_SHORT).show();
                return;
            }
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        } else if (id == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(R.id.seleccion_imagenes);
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
                        Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);

                        // 1. ACTUALIZAR LISTA COMBINADA (imageUriList)
                        List<Uri> listaCombinada = new ArrayList<>(sharedViewModel.getImageUriList());
                        listaCombinada.add(fileUri);
                        sharedViewModel.setImageUriList(listaCombinada);

                        // 2. ACTUALIZAR LISTA SOLO CÁMARA (cameraOnlyUriList)
                        List<Uri> listaCamara = new ArrayList<>(sharedViewModel.getCameraOnlyUriList());
                        listaCamara.add(fileUri);
                        sharedViewModel.setCameraOnlyUriList(listaCamara);

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

    // --- MÉTODOS DE CÁMARA ---

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