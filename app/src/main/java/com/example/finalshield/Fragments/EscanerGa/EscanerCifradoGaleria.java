package com.example.finalshield.Fragments.EscanerGa;

import android.Manifest;
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
import androidx.fragment.app.FragmentResultListener;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.Adaptadores.VistaImagenActivity;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Fragments.Escaner.EscanerProcesador;
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

public class EscanerCifradoGaleria extends Fragment implements View.OnClickListener {

    public static final String KEY_GUARDAR_SELECCION = "guardar_seleccion_key";
    public static final String BUNDLE_REORDENAR_URI_LIST = "reordenar_uri_list_verfotos";
    private static final int REQUEST_CODE = 100;
    private static final String TAG = "EscanerGaleria";

    private SharedImageViewModel sharedViewModel;
    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaImagenes2 = new ArrayList<>();
    private final List<Uri> listaSeleccionadaParaGuardar = new ArrayList<>();

    private TextView selectionCount;
    private Button cancelarSeleccion, guardarFotosSeleccionadas, regresar, guardar;
    private LinearLayout selectionBar;
    ImageButton recortar2, edicion2, eliminar2;

    // SERVICIOS PARA EL CIFRADO
    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedImageViewModel.class);

        // Inicializar servicios
        archivoService = new ArchivoService(requireContext());
        archivoDAO = AppDatabase.getInstance(requireContext()).archivoDAO();

        // ✅ RECEPTOR ORIGINAL MANTENIDO
        getParentFragmentManager().setFragmentResultListener(
                "actualizacion_lista", // Asegúrate que este String coincida con EscanerGaEliminarPaginas.KEY_ACTUALIZACION_LISTA
                this,
                (requestKey, result) -> {
                    ArrayList<String> updatedUriStrings = result.getStringArrayList("uri_list");
                    if (updatedUriStrings != null) {
                        actualizarListaDespuesDeEliminacion(updatedUriStrings);
                    }
                }
        );
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
        guardarFotosSeleccionadas.setOnClickListener(view -> { if (adapter != null && adapter.getSelectedCount() > 0) guardarImagenesSeleccionadas(); });

        recortar2.setOnClickListener(this);
        edicion2.setOnClickListener(this);
        eliminar2.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);

        listaSeleccionadaParaGuardar.clear();
        listaSeleccionadaParaGuardar.addAll(sharedViewModel.getImageUriList());
        pedirPermiso();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        // Aseguramos que el ViewModel tenga la versión más reciente de la lista
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);

        if (id == R.id.recortar2) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                Navigation.findNavController(v).navigate(R.id.escanerGaCortarRotar);
            } else {
                Toast.makeText(getContext(), "No hay imagenes seleccionadas para recortar.", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.edicion2) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                Navigation.findNavController(v).navigate(R.id.escanerGaVisualizacionYReordenamiento);
            } else {
                Toast.makeText(getContext(), "No hay imagenes seleccionadas  para editar.", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.eliminar2) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                Navigation.findNavController(v).navigate(R.id.escanerGaEliminarPaginas);
            } else {
                Toast.makeText(getContext(), "No hay imagenes seleccionadas  para eliminar.", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.regresar3) {
            limpiarCacheLocalTotal();
            Navigation.findNavController(v).navigate(R.id.opcionCifrado2);
        } else if (id == R.id.guardar3) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                // Bloqueamos el botón para evitar clics dobles
                v.setEnabled(false);

                // 1. Capturamos el NavController antes de salir de este fragmento
                final androidx.navigation.NavController navController = Navigation.findNavController(v);

                // 2. Navegamos inmediatamente a la pantalla de carga
                navController.navigate(R.id.cargaProcesos);

                // 3. Iniciamos el proceso pesado (Generar PDF y Enviar)
                EscanerProcesador.generarPdfYEnviar(requireContext(), listaSeleccionadaParaGuardar, archivoService, archivoDAO, () -> {

                    // 4. El proceso terminó. Volvemos al hilo principal para la navegación final
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        try {
                            // Limpiamos la caché antes de movernos
                            limpiarCacheLocalTotal();

                    /* 5. NAVEGACIÓN FINAL
                       Usamos el ID de la ACCIÓN que definiste en tu XML.
                       Esto es mucho más seguro que usar el ID del fragmento directamente.
                    */
                            navController.navigate(R.id.action_cargaProcesos_to_cifradoEscaneo2);

                        } catch (Exception e) {
                            android.util.Log.e("NAV_ERROR", "Error al navegar desde carga: " + e.getMessage());
                            // Si la acción falla, intentamos un respaldo directo al destino
                            try {
                                navController.navigate(R.id.cifradoEscaneo2);
                            } catch (Exception ex) {
                                android.util.Log.e("NAV_ERROR", "Fallo total de navegación: " + ex.getMessage());
                            }
                        }
                    });
                });
            } else {
                Toast.makeText(getContext(), "Debe añadir fotos antes de guardar.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- MÉTODOS DE LÓGICA DE ARCHIVOS (TUS ORIGINALES) ---

    private void limpiarCacheLocalTotal() {
        File[] files = requireContext().getCacheDir().listFiles();
        if (files != null) for (File f : files) if (f.getName().startsWith("editable_")) f.delete();
        listaSeleccionadaParaGuardar.clear();
        sharedViewModel.clearList();
    }

    private void guardarImagenesSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;
        List<Uri> selectedUris = adapter.getSelectedItems();
        int countAdded = 0;
        Set<String> originalIdInMaster = getOriginalIdInMaster(listaSeleccionadaParaGuardar);

        for (Uri galleryUri : selectedUris) {
            String galleryId = galleryUri.getLastPathSegment();
            if (!originalIdInMaster.contains(galleryId)) {
                Uri localEditableUri = obtenerCopiaEditableUnica(galleryUri);
                if (localEditableUri != null) {
                    listaSeleccionadaParaGuardar.add(localEditableUri);
                    originalIdInMaster.add(galleryId);
                    countAdded++;
                }
            }
        }
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);
        adapter.clearSelection();
        actualizarBarra();
        Toast.makeText(getContext(), countAdded + " añadidas. Total: " + listaSeleccionadaParaGuardar.size(), Toast.LENGTH_SHORT).show();
    }

    private Uri obtenerCopiaEditableUnica(Uri galleryUri) {
        try {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String originalSegment = galleryUri.getLastPathSegment();
            String filename = "editable_" + originalSegment + "_" + uniqueId + ".jpg";
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

    private Set<String> getOriginalIdInMaster(List<Uri> masterList) {
        Set<String> ids = new HashSet<>();
        for (Uri uri : masterList) {
            String path = uri.getLastPathSegment();
            if (path != null && path.contains("_")) {
                int start = path.indexOf('_');
                int end = path.lastIndexOf('_');
                if (start != -1 && end > start) ids.add(path.substring(start + 1, end));
            }
        }
        return ids;
    }

    private void actualizarListaDespuesDeEliminacion(ArrayList<String> updatedUriStrings) {
        listaSeleccionadaParaGuardar.clear();
        for (String s : updatedUriStrings) listaSeleccionadaParaGuardar.add(Uri.parse(s));
        sharedViewModel.setImageUriList(listaSeleccionadaParaGuardar);
    }

    // --- PERMISOS Y CARGA (MANTENIDO) ---
    private void pedirPermiso() {
        String p = (Build.VERSION.SDK_INT >= 33) ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED) cargarImagenes();
        else requestPermissions(new String[]{p}, REQUEST_CODE);
    }

    private void cargarImagenes() {
        listaImagenes2.clear();
        Uri coll = (Build.VERSION.SDK_INT >= 29) ? MediaStore.Images.Media.getContentUri("external") : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor c = requireActivity().getContentResolver().query(coll, new String[]{MediaStore.Images.Media._ID}, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (c != null) {
                int idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (c.moveToNext()) listaImagenes2.add(ContentUris.withAppendedId(coll, c.getLong(idCol)));
            }
        } catch (Exception e) { Log.e(TAG, "Error MediaStore", e); }
        adapter = new ImageAdapter(listaImagenes2, new ImageAdapter.Callbacks() {
            @Override public void onImageClicked(Uri uri) {}
            @Override public void onSelectionChanged(int count) { actualizarBarra(); }
        }, R.layout.item_imagen, true);
        recyclerViee.setAdapter(adapter);
    }

    private void actualizarBarra() {
        int count = (adapter != null) ? adapter.getSelectedCount() : 0;
        selectionBar.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) selectionCount.setText(count + " seleccionadas");
    }
}