package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
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
import android.widget.Toast;

import com.example.finalshield.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;


public class EscanerCifradoMixto extends Fragment implements View.OnClickListener {
    ImageButton galeria, addele,recortar, edicion, eliminar;
    // esta es la ventana donde se verá la cámara (nuestro recuadro negro).
    private PreviewView vistaPrevia;
    // esto es lo que nos dará acceso real a la cámara, cuando Android quiera.
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    // un ID para identificar la petición de permiso de cámara.
    private static final int CAMERA_REQUEST_CODE = 10;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_mixto, container, false);
    }
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        vistaPrevia = v.findViewById(R.id.vistaprevia);
        Button regre = v.findViewById(R.id.regresar1);
        galeria = v.findViewById(R.id.selecgaleria);
        addele = v.findViewById(R.id.addelements);
        recortar = v.findViewById(R.id.recortar);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);
        galeria.setOnClickListener(this);
        addele.setOnClickListener(this);
        recortar.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regre.setOnClickListener(this);
        // checar si el usuario ya nos dio permiso
        if (allPermissionsGranted()) {
            // si si, encendemos la cámara.
            startCamera();
        } else {
            // si no mostramos la ventanita para pedir permiso.
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.regresar1){
            Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        } else if (v.getId() == R.id.selecgaleria) {
            Navigation.findNavController(v).navigate(R.id.seleccion_imagenes);
        } else if (v.getId() == R.id.addelements) {
            Navigation.findNavController(v).navigate(R.id.escanearMasPaginas);
        } else if (v.getId() == R.id.recortar) {
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        } else if (v.getId() == R.id.edicion) {
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (v.getId() == R.id.eliminar) {
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        }
    }
    // 1. verificar el permiso
    private boolean allPermissionsGranted() {
        // Comprobamos si el permiso de cámara está en "Permitido".
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    // 2. manejamos la respuesta de lo de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Nos aseguramos de que la respuesta sea para nuestra solicitud de cámara.
        if (requestCode == CAMERA_REQUEST_CODE) {
            // Si después de la respuesta, el permiso SÍ se concedió (revisamos otra vez):
            if (allPermissionsGranted()) {
                startCamera(); //Encendemos la cámara.
            } else {
                // Si el usuario dijo "No", le avisamos con un mensajito rápido.
                Toast.makeText(requireContext(), "Permiso de cámara no concedido.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // 3. la funcion de iniciar la camara con el camarax
    private void startCamera() {
        // pedimos el motor principal de CameraX.
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        // cuando el motor esté listo ejecuta este código:
        cameraProviderFuture.addListener(() -> {
            try {
                // motor listo
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // 1. configuramos la vista previa el "caso de uso" para mostrar video.
                Preview preview = new Preview.Builder().build();
                // 2. le decimos que queremos usar la cámara de atrás.
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                // 3. conectamos el video en vivo al visor
                preview.setSurfaceProvider(vistaPrevia.getSurfaceProvider());
                // (extra) Configuramos la herramienta para poder tomar la foto después.
                ImageCapture imageCapture = new ImageCapture.Builder().build();
                // 4. Conectamos lo anterior
                // primero, limpiamos cualquier conexión anterior.
                cameraProvider.unbindAll();
                // luego, le decimos a CameraX: "Usa el visor, la cámara trasera y la herramienta de foto,
                // y que esto se apague cuando el fragmento se cierre."
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector,preview,imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                // si algo se falla al intentar prender la cámara, lo registramos.
                Log.e("EscanerCifrado", "Error al inicializar CameraX.", e);
            }
        }, ContextCompat.getMainExecutor(requireContext())); // nos aseguramos que corra en el hilo principal
    }
}