package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Fragments.Escaner.EscanerProcesador;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.ViewModel.CargaViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EscanerCifradoMixto extends Fragment implements View.OnClickListener {

    private static final String FOTO_PREFIX = "CAMARA_TEMP_";
    private static final String TAG = "EscanerMixto";

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            });

    private ImageButton recortar, eliminar, selectimg, acomodar;
    private Button regresar, guardarPdf, tomarfoto;
    private PreviewView vistaPrevia;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ImageView imagencita;
    private TextView contadorfot;

    private SharedImageViewModel sharedViewModel;
    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;
    private CargaViewModel cargaViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);
        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_mixto, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        vistaPrevia    = v.findViewById(R.id.vistaprevia);
        regresar       = v.findViewById(R.id.regresar4);
        guardarPdf     = v.findViewById(R.id.guardar);
        tomarfoto      = v.findViewById(R.id.tomarfoto);
        imagencita     = v.findViewById(R.id.imagencita);
        contadorfot    = v.findViewById(R.id.Contadorfot);
        selectimg      = v.findViewById(R.id.selecgaleria);
        acomodar       = v.findViewById(R.id.edicion);
        recortar       = v.findViewById(R.id.recortar);
        eliminar       = v.findViewById(R.id.eliminar);

        tomarfoto.setOnClickListener(this);
        if (imagencita != null) imagencita.setOnClickListener(this);
        recortar.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regresar.setOnClickListener(this);
        selectimg.setOnClickListener(this);
        acomodar.setOnClickListener(this);
        guardarPdf.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        recontarFotosDesdeCache();
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarUIConDatosActuales();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);

        if (id == R.id.tomarfoto) {
            tomarFoto();
        }
        else if (id == R.id.guardar) {
            procesarYGuardarMixto(nav);
        }
        else if (id == R.id.regresar4) {
            limpiarArchivosDeSesionAnterior();
            nav.navigate(R.id.opcionCifrado2);
        }
        else if (id == R.id.selecgaleria) {
            nav.navigate(R.id.seleccion_imagenes);
        }
        else if (id == R.id.imagencita) {
            if (!sharedViewModel.getCameraOnlyUriList().isEmpty()) {
                nav.navigate(R.id.escanerReordenarFotosTomadas);
            }
        }
        else if (id == R.id.recortar || id == R.id.edicion || id == R.id.eliminar) {
            if (!sharedViewModel.getImageUriList().isEmpty()) {
                int dest = (id == R.id.recortar) ? R.id.cortarRotar :
                        (id == R.id.edicion) ? R.id.visualizacionYReordenamiento : R.id.eliminarPaginas;
                nav.navigate(dest);
            }
        }
    }

    private void procesarYGuardarMixto(NavController nav) {
        List<Uri> todasLasUris = sharedViewModel.getImageUriList();
        if (todasLasUris.isEmpty()) {
            Toast.makeText(requireContext(), "No hay imágenes para procesar", Toast.LENGTH_SHORT).show();
            return;
        }

        guardarPdf.setEnabled(false);

        // Referencias finales para el proceso en background
        final CargaViewModel vm = this.cargaViewModel;
        final Context appCtx = requireContext().getApplicationContext();

        // 1. Navegar a pantalla de carga inmediatamente indicando el destino final
        Bundle args = new Bundle();
        args.putInt("destino_final", R.id.cifradoEscaneo2);
        nav.navigate(R.id.cargaProcesos, args);

        // 2. Ejecutar procesamiento
        // IMPORTANTE: Asegúrate que EscanerProcesador no llame al callback inmediatamente
        EscanerProcesador.generarPdfYEnviar(
                appCtx,
                new ArrayList<>(todasLasUris),
                archivoService,
                archivoDAO,
                () -> { // ESTO SE EJECUTA CUANDO EL PDF SE SUBIÓ Y GUARDÓ EN DB
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Primero marcamos como terminado para que la pantalla de carga
                        // muestre el éxito y navegue por su cuenta usando el "destino_final"
                        vm.terminarProceso();

                        // Limpieza después de que la señal de éxito fue enviada
                        borrarFotosDeTodoElDispositivo(todasLasUris);
                        sharedViewModel.clearList();
                        sharedViewModel.clearCameraOnlyList();
                        limpiarArchivosDeSesionAnterior();

                        Log.d(TAG, "Cifrado Mixto Completado con éxito");
                    }, 800); // El delay ayuda a que la animación de carga se vea natural
                },
                errorMsg -> { // ERROR real
                    new Handler(Looper.getMainLooper()).post(() -> {
                        vm.resetear();
                        if (isAdded()) {
                            nav.popBackStack(); // Regresa al escaner para intentar de nuevo
                            guardarPdf.setEnabled(true);
                            Toast.makeText(appCtx, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
        );
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
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Fallo al iniciar cámara", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void tomarFoto() {
        if (imageCapture == null) return;

        File photoFile = new File(requireContext().getCacheDir(), FOTO_PREFIX + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                if (!isAdded()) return;

                Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);

                List<Uri> listaCombinada = new ArrayList<>(sharedViewModel.getImageUriList());
                listaCombinada.add(fileUri);
                sharedViewModel.setImageUriList(listaCombinada);

                List<Uri> listaCamara = new ArrayList<>(sharedViewModel.getCameraOnlyUriList());
                listaCamara.add(fileUri);
                sharedViewModel.setCameraOnlyUriList(listaCamara);

                actualizarUIConDatosActuales();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (isAdded()) Toast.makeText(requireContext(), "Error al capturar foto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void limpiarArchivosDeSesionAnterior() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) ->
                (name.startsWith(FOTO_PREFIX) || name.startsWith("EDITADO_") || name.startsWith("editable_"))
                        && name.endsWith(".jpg"));

        if (files != null) {
            for (File file : files) file.delete();
        }
        limpiarUI();
    }

    private void actualizarUIConDatosActuales() {
        List<Uri> listaCamara = sharedViewModel.getCameraOnlyUriList();
        int size = listaCamara.size();
        if (contadorfot != null) contadorfot.setText(String.valueOf(size));

        if (!listaCamara.isEmpty()) {
            Uri lastUri = listaCamara.get(size - 1);
            Glide.with(this)
                    .load(lastUri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .into(imagencita);
        } else {
            limpiarUI();
        }
    }

    private void limpiarUI() {
        if (contadorfot != null) contadorfot.setText("0");
        if (imagencita != null) Glide.with(this).load((String) null).into(imagencita);
    }

    private void recontarFotosDesdeCache() {
        File cacheDir = requireContext().getCacheDir();
        File[] cachedFiles = cacheDir.listFiles((dir, name) ->
                (name.startsWith(FOTO_PREFIX) || name.startsWith("EDITADO_") || name.startsWith("editable_")) && name.endsWith(".jpg"));

        if (cachedFiles != null && cachedFiles.length > 0) {
            List<File> tempFiles = new ArrayList<>(List.of(cachedFiles));
            tempFiles.sort(Comparator.comparingLong(File::lastModified));

            List<Uri> todasUris = new ArrayList<>();
            List<Uri> camaraUris = new ArrayList<>();

            for (File f : tempFiles) {
                Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", f);
                todasUris.add(uri);
                if (f.getName().startsWith(FOTO_PREFIX)) camaraUris.add(uri);
            }

            sharedViewModel.setImageUriList(todasUris);
            sharedViewModel.setCameraOnlyUriList(camaraUris);
        }
        actualizarUIConDatosActuales();
    }

    private void borrarFotosDeTodoElDispositivo(List<Uri> uris) {
        if (getContext() == null) return;
        ContentResolver resolver = requireContext().getContentResolver();
        for (Uri uri : uris) {
            try {
                if (uri.toString().contains(requireContext().getPackageName())) {
                    File f = getFileFromUri(uri);
                    if (f != null && f.exists()) f.delete();
                }
                resolver.delete(uri, null, null);
            } catch (Exception ignored) {}
        }
    }

    private File getFileFromUri(Uri uri) {
        if (uri == null || getContext() == null) return null;
        String name = uri.getLastPathSegment();
        if (name == null) return null;
        return new File(requireContext().getCacheDir(), name);
    }
}