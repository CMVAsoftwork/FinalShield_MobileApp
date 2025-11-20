package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.pm.PackageManager;
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

public class EscanerCifradoMixto extends Fragment implements View.OnClickListener {

    // prefijo para identificar las fotos de la sesión en la caché
    private static final String FOTO_PREFIX = "MIXTO_TEMP_";
    // variables de la UI
    ImageButton addele, recortar, edicion, eliminar, galeria;
    Button regresar, guardarPdf;
    private PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private Button tomarfoto;
    private ImageView imagencita;
    private TextView contadorfot;
    // variables de la Lógica
    private int contador = 0;
    private List<File> fotosTomadas = new ArrayList<>();
    private static final int CAMERA_REQUEST_CODE = 10;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_mixto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        // Si el estado es nulo (primera vez), se asume que se debe iniciar una nueva sesión
        // y limpiar. Si NO es nulo, significa que se está restaurando el estado,
        // y onResume se encargará de recontar.
        // onResume re-cuente siempre, a menos que el botón "Regresar" sea presionado.
        vistaPrevia = v.findViewById(R.id.vistaprevia);
        regresar = v.findViewById(R.id.regresar4);
        guardarPdf = v.findViewById(R.id.guardar);
        tomarfoto = v.findViewById(R.id.tomarfoto);
        imagencita = v.findViewById(R.id.imagencita);
        contadorfot = v.findViewById(R.id.Contadorfot);
        addele = v.findViewById(R.id.addelements);
        recortar = v.findViewById(R.id.recortar);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);
        galeria = v.findViewById(R.id.selecgaleria);

        regresar.setOnClickListener(this);
        tomarfoto.setOnClickListener(this);
        if (imagencita != null) imagencita.setOnClickListener(this);
        addele.setOnClickListener(this);
        recortar.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        galeria.setOnClickListener(this);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    // esta función solo se usa ahora para el borrado explícito .
    private void limpiarArchivosDeSesionAnterior() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith(FOTO_PREFIX) && name.endsWith(".jpg"));
        if (files != null) {
            for (File file : files) {
                if (file.delete()) {
                    Log.d("MixtoCache", "Archivo de sesión eliminado: " + file.getName());
                }
            }
        }
        // reinicializa el estado interno
        contador = 0;
        fotosTomadas.clear();
        if (contadorfot != null) actualizarContadorUI();
        if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
    }

    //se llama cada vez que el fragmento vuelve a ser visible (al regresar de VerFotosTomadas)
    @Override
    public void onResume() {
        super.onResume();
        // lee el disco y restablece el estado del contador.
        recontarFotosDesdeCache();
    }
    private void recontarFotosDesdeCache() {
        fotosTomadas.clear();
        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                // filtra solo los archivos que tienen nuestro prefijo (los que no se eliminaron en VerFotosTomadas)
                if (file.isFile() && file.getName().startsWith(FOTO_PREFIX) && file.getName().endsWith(".jpg")) {
                    fotosTomadas.add(file);
                }
            }
        }
        contador = fotosTomadas.size();
        actualizarContadorUI();
        if (!fotosTomadas.isEmpty()) {
            // mostrar la última foto de la lista restante
            mostrarUltimaFoto(fotosTomadas.get(fotosTomadas.size() - 1));
        } else {
            // B}borra la imagen si la lista está vacía
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

                Navigation.findNavController(v).navigate(R.id.verFotosTomadas, bundle);
            } else {
                Toast.makeText(requireContext(), "No hay fotos para visualizar.", Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.regresar4){
            // si el usuario presiona regresar, borramos los archivos.
            limpiarArchivosDeSesionAnterior();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (id == R.id.tomarfoto) {
            tomarFoto();
        } else if (id == R.id.addelements) {
            Navigation.findNavController(v).navigate(R.id.escanearMasPaginas);
        } else if (id == R.id.recortar) {
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        } else if (id == R.id.edicion) {
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (id == R.id.eliminar) {
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        } else if (id == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(R.id.seleccion_imagenes);
        }
    }

    // función para Tomar la Foto
    private void tomarFoto() {
        if (imageCapture == null) {
            Log.e("EscanerCifrado", "ImageCapture no está inicializado.");
            Toast.makeText(requireContext(), "Cámara no lista.", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDirectory = requireContext().getCacheDir();
        // usar el prefijo en el nombre del archivo
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
    // función para Mostrar la ultima foto
    private void mostrarUltimaFoto(File photoFile) {
        Glide.with(this)
                .load(photoFile)
                .centerCrop()
                .into(imagencita);
    }
    //verificar el permiso
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    // manejar la respuesta de los permisos
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

    //función para Iniciar la Cámara
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