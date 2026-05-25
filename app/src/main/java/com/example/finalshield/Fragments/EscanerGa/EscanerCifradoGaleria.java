package com.example.finalshield.Fragments.EscanerGa;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Fragments.Escaner.EscanerProcesador;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.Service.CifradoWorker;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class EscanerCifradoGaleria extends Fragment implements View.OnClickListener {

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final String TAG = "EscanerGaleria";

    private SharedImageViewModel sharedViewModel;
    private CargaViewModel cargaViewModel;
    private RecyclerView recyclerViee;
    private ImageAdapter adapter;

    private final List<Uri> listaImagenesGaleria = new ArrayList<>();
    private final List<Uri> listaSeleccionadaParaGuardar = new ArrayList<>();
    private final List<Uri> listaOrigenesGaleria = new ArrayList<>();

    private TextView selectionCount;
    private Button cancelarSeleccion, guardarFotosSeleccionadas, regresar, guardar;
    private LinearLayout selectionBar;
    private ImageButton recortar2, edicion2, eliminar2;

    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    ejecutarCifradoFinal();
                } else {
                    guardar.setEnabled(true);
                    Toast.makeText(getContext(), "Proceso cancelado: Se requiere permiso para limpiar originales", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);
        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        getParentFragmentManager().setFragmentResultListener("actualizacion_lista", this,
                (requestKey, result) -> {
                    ArrayList<String> updatedUriStrings = result.getStringArrayList("uri_list");
                    if (updatedUriStrings != null) {
                        actualizarListasDesdeResultado(updatedUriStrings);
                    }
                });
    }

    private void actualizarListasDesdeResultado(ArrayList<String> updatedUris) {
        listaSeleccionadaParaGuardar.clear();
        for (String s : updatedUris) {
            listaSeleccionadaParaGuardar.add(Uri.parse(s));
        }
        sharedViewModel.setImageUriList(new ArrayList<>(listaSeleccionadaParaGuardar));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_escaner_cifrado_galeria, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        recyclerViee = v.findViewById(R.id.recycler);
        recyclerViee.setLayoutManager(new GridLayoutManager(getContext(), 2));
        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        cancelarSeleccion = v.findViewById(R.id.clearSelection);
        guardarFotosSeleccionadas = v.findViewById(R.id.guardarSeleccion);
        recortar2 = v.findViewById(R.id.recortar2);
        edicion2 = v.findViewById(R.id.edicion2);
        eliminar2 = v.findViewById(R.id.eliminar2);
        regresar = v.findViewById(R.id.regresar3);
        guardar = v.findViewById(R.id.guardar3);

        cancelarSeleccion.setOnClickListener(view -> { if (adapter != null) { adapter.clearSelection(); actualizarBarra(); } });
        guardarFotosSeleccionadas.setOnClickListener(view -> guardarImagenesSeleccionadas());
        recortar2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        List<Uri> current = sharedViewModel.getImageUriList();
        if (current != null && !current.isEmpty()) {
            listaSeleccionadaParaGuardar.clear();
            listaSeleccionadaParaGuardar.addAll(current);
        }

        pedirPermisos();
    }

    private void guardarImagenesSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> selectedUris = adapter.getSelectedItems();
        for (Uri galleryUri : selectedUris) {
            if (!listaOrigenesGaleria.contains(galleryUri)) {
                Uri localUri = obtenerCopiaEditableUnica(galleryUri);
                if (localUri != null) {
                    listaSeleccionadaParaGuardar.add(localUri);
                    listaOrigenesGaleria.add(galleryUri);
                }
            }
        }
        sharedViewModel.setImageUriList(new ArrayList<>(listaSeleccionadaParaGuardar));
        adapter.clearSelection();
        actualizarBarra();
        Toast.makeText(getContext(), "Añadidas a la cola", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);

        if (id == R.id.guardar3) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                solicitarPermisoBorradoYProcesar();
            } else {
                Toast.makeText(getContext(), "Selecciona imágenes primero", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.regresar3) {
            limpiarCacheLocalTotal();
            nav.navigate(R.id.opcionCifrado2);
        } else if (id == R.id.recortar2 || id == R.id.edicion2 || id == R.id.eliminar2) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                sharedViewModel.setImageUriList(new ArrayList<>(listaSeleccionadaParaGuardar));
                int dest = (id == R.id.recortar2) ? R.id.escanerGaCortarRotar :
                        (id == R.id.edicion2) ? R.id.escanerGaVisualizacionYReordenamiento : R.id.escanerGaEliminarPaginas;
                nav.navigate(dest);
            }
        }
    }

    private void solicitarPermisoBorradoYProcesar() {
        if (listaOrigenesGaleria.isEmpty()) {
            ejecutarCifradoFinal();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PendingIntent pendingIntent = MediaStore.createDeleteRequest(requireContext().getContentResolver(), listaOrigenesGaleria);
                deleteLauncher.launch(new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
            } else {
                ejecutarCifradoFinal();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error permiso borrado", e);
            ejecutarCifradoFinal();
        }
    }

    private void ejecutarCifradoFinal() {
        guardar.setEnabled(false);
        NavController nav = Navigation.findNavController(requireView());

        final CargaViewModel vm = this.cargaViewModel;
        final Context appCtx = requireContext().getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Armamos primero el PDF en disco dentro del caché de la app
                String nombreLimpioFinal = "cif_SCAN_GAL_" + (System.currentTimeMillis() / 1000) + ".pdf";
                File outputPdfLocal = new File(appCtx.getCacheDir(), nombreLimpioFinal);

                EscanerProcesador.compilarPdfLocalDesdeUris(appCtx, new ArrayList<>(listaSeleccionadaParaGuardar), outputPdfLocal);

                if (!outputPdfLocal.exists() || outputPdfLocal.length() == 0) {
                    throw new java.io.FileNotFoundException("Fallo al compilar el contenedor binario del PDF.");
                }

                // 2. Ahora que el PDF ya está inflado con su estructura real, calculamos su peso exacto
                long tamanoRealPdfBytes = outputPdfLocal.length();
                long limiteBytes = 15 * 1024 * 1024; // 15 MB
                boolean conectado = tieneInternet();

                // 3. Evaluamos la condición con el peso real del archivo final
                if (tamanoRealPdfBytes >= limiteBytes || !conectado) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        String msg = !conectado
                                ? "Sin conexión. Armando escaneo para subirlo en segundo plano..."
                                : "Escaneo grande detectado. Procesando en segundo plano...";
                        Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show();
                    });

                    // Despachamos la ruta absoluta directa del archivo
                    String[] inputUris = new String[]{outputPdfLocal.getAbsolutePath()};

                    Data dataWorker = new Data.Builder()
                            .putStringArray("uris_llave", inputUris)
                            .putString("id_usuario_llave", "18")
                            .putString("nombre_visual_limpio", nombreLimpioFinal)
                            .putString("origen_boveda", "ESCANEO") // Filtro de enrutamiento asignado
                            .build();

                    Constraints restricciones = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build();

                    OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CifradoWorker.class)
                            .setInputData(dataWorker)
                            .setConstraints(restricciones)
                            .build();

                    WorkManager.getInstance(appCtx).enqueue(request);

                    // Limpieza limpia de las fotos de la galería original en dispositivos antiguos
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        for (Uri u : listaOrigenesGaleria) {
                            try { appCtx.getContentResolver().delete(u, null, null); } catch(Exception ignored){}
                        }
                    }

                    borrarTemporalesExhaustivo(listaSeleccionadaParaGuardar);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        limpiarCacheLocalTotal();
                        nav.navigate(R.id.cifradoEscaneo2);
                    });

                } else {

                    new Handler(Looper.getMainLooper()).post(() -> {
                        vm.resetear();
                        Bundle args = new Bundle();
                        args.putInt("destino_final", R.id.cifradoEscaneo2);
                        nav.navigate(R.id.cargaProcesos, args);
                    });

                    // Como el PDF ya está generado en disco y mide menos de 15MB, se lo pasamos directo al cargador síncrono
                    EscanerProcesador.generarPdfYEnviar(
                            appCtx,
                            new ArrayList<>(listaSeleccionadaParaGuardar),
                            archivoService,
                            archivoDAO,
                            () -> {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                    for (Uri u : listaOrigenesGaleria) {
                                        try { appCtx.getContentResolver().delete(u, null, null); } catch(Exception ignored){}
                                    }
                                }
                                borrarTemporalesExhaustivo(listaSeleccionadaParaGuardar);

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    vm.terminarProceso();
                                    limpiarCacheLocalTotal();
                                    Log.d(TAG, "Cifrado Galería Exitoso en primer plano");
                                }, 800);
                            },
                            errorMsg -> {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    vm.resetear();
                                    if (isAdded()) {
                                        nav.popBackStack();
                                        guardar.setEnabled(true);
                                        Toast.makeText(appCtx, "Error en carga síncrona: " + errorMsg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error crítico procesando compilación estratégica: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> guardar.setEnabled(true));
            }
        });
    }

    private void borrarTemporalesExhaustivo(List<Uri> uris) {
        if (getContext() == null) return;
        ContentResolver resolver = requireContext().getContentResolver();
        for (Uri uri : uris) {
            try {
                if (uri.toString().contains(requireContext().getPackageName())) {
                    resolver.delete(uri, null, null);
                }
                File f = getFileFromUri(uri);
                if (f != null && f.exists()) f.delete();
            } catch (Exception e) {
                Log.e(TAG, "Error borrando copia temporal: " + uri, e);
            }
        }
    }

    private File getFileFromUri(Uri uri) {
        if (uri == null || getContext() == null) return null;
        String fileName = uri.getLastPathSegment();
        if (fileName == null) return null;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        return new File(requireContext().getCacheDir(), fileName);
    }

    private boolean tieneInternet() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void limpiarCacheLocalTotal() {
        try {
            if (getContext() == null) return;
            File cacheDir = requireContext().getCacheDir();
            File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("editable_"));
            if (files != null) for (File f : files) f.delete();
            listaSeleccionadaParaGuardar.clear();
            listaOrigenesGaleria.clear();
            sharedViewModel.setImageUriList(new ArrayList<>());
        } catch (Exception e) { Log.e(TAG, "Error limpiando cache", e); }
    }

    private Uri obtenerCopiaEditableUnica(Uri galleryUri) {
        try {
            String filename = "editable_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            File file = new File(requireContext().getCacheDir(), filename);
            try (InputStream is = requireContext().getContentResolver().openInputStream(galleryUri);
                 FileOutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) os.write(buffer, 0, read);
            }
            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        } catch (Exception e) { return null; }
    }

    private void pedirPermisos() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_PERMISSIONS);
            } else { cargarImagenes(); }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            } else { cargarImagenes(); }
        }
    }

    private void cargarImagenes() {
        listaImagenesGaleria.clear();
        Uri coll = (Build.VERSION.SDK_INT >= 29) ? MediaStore.Images.Media.getContentUri("external") : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = requireActivity().getContentResolver().query(coll, new String[]{MediaStore.Images.Media._ID}, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    listaImagenesGaleria.add(ContentUris.withAppendedId(coll, cursor.getLong(idCol)));
                }
            }
        } catch (Exception e) { Log.e(TAG, "Error cargando", e); }

        adapter = new ImageAdapter(listaImagenesGaleria, new ImageAdapter.Callbacks() {
            @Override public void onImageClicked(Uri uri) {}
            @Override public void onSelectionChanged(int count) { actualizarBarra(); }
        }, R.layout.item_imagen, true);
        recyclerViee.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] p, @NonNull int[] rs) {
        if (rc == REQUEST_CODE_PERMISSIONS && rs.length > 0 && rs[0] == PackageManager.PERMISSION_GRANTED) {
            cargarImagenes();
        }
    }

    private void actualizarBarra() {
        int count = (adapter != null) ? adapter.getSelectedCount() : 0;
        selectionBar.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) selectionCount.setText(String.valueOf(count) + " seleccionadas");
    }
}