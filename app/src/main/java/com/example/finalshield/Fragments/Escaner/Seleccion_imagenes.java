package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Seleccion_imagenes extends Fragment implements View.OnClickListener {
    ImageButton camara, recortar, edicion, eliminar;
    private Button regresar;
    private Button guardar;

    private static final int REQUEST_CODE = 100;
    private static final String TAG = "EscanerGaleria";

    private SharedImageViewModel sharedViewModel;

    // ✅ SERVICIOS AÑADIDOS PARA EL CIFRADO
    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;

    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaMixta = new ArrayList<>();

    private TextView selectionCount;
    private Button cancelarSeleccion;
    private Button guardarFotosSeleccionadas;
    private LinearLayout selectionBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);

        // ✅ Inicializar servicios
        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seleccion_imagenes, container, false);
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

        recortar = v.findViewById(R.id.recortar);
        camara = v.findViewById(R.id.scann);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);
        regresar = v.findViewById(R.id.regresar);
        guardar = v.findViewById(R.id.guardar);

        cancelarSeleccion.setOnClickListener(view -> {
            if (adapter != null) {
                adapter.clearSelection();
                actualizarBarra();
            }
        });

        guardarFotosSeleccionadas.setOnClickListener(view -> {
            if (adapter != null && adapter.getSelectedCount() > 0) {
                guardarImagenesSeleccionadas();
            } else {
                Toast.makeText(getContext(), "No hay fotos seleccionadas.", Toast.LENGTH_SHORT).show();
            }
        });

        recortar.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);
        camara.setOnClickListener(this);

        pedirPermiso();
    }

    // --- LÓGICA DE CIFRADO Y BORRADO (CORRECCIÓN SOLICITADA) ---

    private void ejecutarCifradoYBorrado(View v) {
        List<Uri> listaActual = new ArrayList<>(sharedViewModel.getImageUriList());

        if (listaActual.isEmpty()) {
            Toast.makeText(requireContext(), "No hay fotos para cifrar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Bloquear UI y navegar a Carga
        v.setEnabled(false);
        final androidx.navigation.NavController navController = Navigation.findNavController(v);
        navController.navigate(R.id.cargaProcesos);

        // 2. Iniciar proceso de PDF y Cifrado
        EscanerProcesador.generarPdfYEnviar(
                requireContext(),
                listaActual,
                archivoService,
                archivoDAO,
                () -> {
                    // ✅ 3. BORRAR ORIGINALES DE LA GALERÍA
                    borrarFotosDeTodoElDispositivo(listaActual);

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        // 4. Limpiar caché de la sesión
                        limpiarArchivosTemporales();

                        // 5. Navegar al éxito
                        navController.navigate(R.id.action_cargaProcesos_to_cifradoEscaneo2);
                        Toast.makeText(getContext(), "Cifrado completado. Galería limpia.", Toast.LENGTH_LONG).show();
                    });
                }
        );
    }

    private void borrarFotosDeTodoElDispositivo(List<Uri> uris) {
        ContentResolver resolver = requireContext().getContentResolver();
        for (Uri uri : uris) {
            try {
                // Borrar del MediaStore (esto borra el archivo real del teléfono)
                resolver.delete(uri, null, null);
            } catch (Exception e) {
                Log.e(TAG, "No se pudo borrar el original: " + uri);
            }
        }
        // Forzar actualización para que Google Fotos se refresque
        if (!uris.isEmpty()) {
            requireContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uris.get(0)));
        }
    }

    private void limpiarArchivosTemporales() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("editable_") && name.endsWith(".jpg"));
        if (files != null) for (File file : files) file.delete();
        sharedViewModel.clearList();
        sharedViewModel.clearCameraOnlyList();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.guardar) {
            ejecutarCifradoYBorrado(v);
        } else if (id == R.id.scann) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        } else if (id == R.id.recortar) {
            if (sharedViewModel.getImageUriList().isEmpty()) return;
            Navigation.findNavController(v).navigate(R.id.cortarRotar);
        } else if (id == R.id.edicion) {
            if (sharedViewModel.getImageUriList().isEmpty()) return;
            Navigation.findNavController(v).navigate(R.id.visualizacionYReordenamiento);
        } else if (id == R.id.eliminar) {
            if (sharedViewModel.getImageUriList().isEmpty()) return;
            Navigation.findNavController(v).navigate(R.id.eliminarPaginas);
        } else if (id == R.id.regresar) {
            Navigation.findNavController(v).navigate(R.id.escanerCifradoMixto);
        }
    }

    // --- MÉTODOS DE SOPORTE (CARGAR IMÁGENES, PERMISOS, ETC) ---

    private void pedirPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE);
        else
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cargarImagenes();
        }
    }

    private void cargarImagenes() {
        listaMixta.clear();
        Uri collection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        try (Cursor cursor = requireActivity().getContentResolver().query(
                collection, new String[]{MediaStore.Images.Media._ID}, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    listaMixta.add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)));
                }
            }
        } catch (Exception e) { Log.e(TAG, "Error carga: " + e.getMessage()); }

        adapter = new ImageAdapter(listaMixta, new ImageAdapter.Callbacks() {
            @Override public void onImageClicked(Uri uri) {}
            @Override public void onSelectionChanged(int count) { actualizarBarra(); }
        }, R.layout.item_imagen, true);
        recyclerViee.setAdapter(adapter);
    }

    private void guardarImagenesSeleccionadas() {
        List<Uri> selectedUris = adapter.getSelectedItems();
        List<Uri> masterList = new ArrayList<>(sharedViewModel.getImageUriList());
        for (Uri uri : selectedUris) {
            Uri copy = obtenerCopiaEditableUnica(uri);
            if (copy != null) masterList.add(copy);
        }
        sharedViewModel.setImageUriList(masterList);
        adapter.clearSelection();
        actualizarBarra();
        Toast.makeText(getContext(), "Fotos añadidas a la lista de cifrado", Toast.LENGTH_SHORT).show();
    }

    private Uri obtenerCopiaEditableUnica(Uri galleryUri) {
        try {
            File file = new File(requireContext().getCacheDir(), "editable_" + System.currentTimeMillis() + ".jpg");
            InputStream is = requireContext().getContentResolver().openInputStream(galleryUri);
            FileOutputStream os = new FileOutputStream(file);
            byte[] buf = new byte[1024]; int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close(); is.close();
            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        } catch (Exception e) { return null; }
    }

    private void actualizarBarra() {
        int count = (adapter != null) ? adapter.getSelectedCount() : 0;
        selectionBar.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        selectionCount.setText(count + " seleccionadas");
    }
}