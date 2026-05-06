package com.example.finalshield.Fragments.Escaner;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

import com.example.finalshield.Adaptadores.ImageAdapter;
import com.example.finalshield.Adaptadores.SharedImageViewModel;
import com.example.finalshield.DBM.AppDatabase;
import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.Fragments.Escaner.EscanerProcesador;
import com.example.finalshield.R;
import com.example.finalshield.Service.ArchivoService;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Seleccion_imagenes extends Fragment implements View.OnClickListener {

    private static final int REQUEST_CODE = 100;
    private static final String TAG = "SeleccionImagenes";

    private SharedImageViewModel sharedViewModel;
    private CargaViewModel cargaViewModel;
    private ArchivoService archivoService;
    private ArchivoDAO archivoDAO;

    private RecyclerView recyclerViee;
    private ImageAdapter adapter;
    private final List<Uri> listaMixta = new ArrayList<>();

    private final List<Uri> listaSeleccionadaParaGuardar = new ArrayList<>();
    private final List<Uri> listaOrigenesGaleria = new ArrayList<>();

    private TextView selectionCount;
    private Button cancelarSeleccion, guardarFotosSeleccionadas, regresar, guardar;
    private LinearLayout selectionBar;
    private ImageButton recortar, edicion, eliminar, camara;

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
        return inflater.inflate(R.layout.fragment_seleccion_imagenes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        vincularVistas(v);

        List<Uri> actuales = sharedViewModel.getImageUriList();
        if (actuales != null && !actuales.isEmpty()) {
            listaSeleccionadaParaGuardar.clear();
            listaSeleccionadaParaGuardar.addAll(actuales);
        }

        pedirPermiso();
    }

    private void vincularVistas(View v) {
        recyclerViee = v.findViewById(R.id.recycler);
        recyclerViee.setLayoutManager(new GridLayoutManager(getContext(), 2));
        selectionBar = v.findViewById(R.id.selectionBar);
        selectionCount = v.findViewById(R.id.selectionCount);
        cancelarSeleccion = v.findViewById(R.id.clearSelection);
        guardarFotosSeleccionadas = v.findViewById(R.id.guardarSeleccion);
        recortar = v.findViewById(R.id.recortar);
        edicion = v.findViewById(R.id.edicion);
        eliminar = v.findViewById(R.id.eliminar);
        regresar = v.findViewById(R.id.regresar);
        guardar = v.findViewById(R.id.guardar);
        camara = v.findViewById(R.id.scann);

        cancelarSeleccion.setOnClickListener(view -> { if (adapter != null) { adapter.clearSelection(); actualizarBarra(); } });
        guardarFotosSeleccionadas.setOnClickListener(view -> guardarImagenesSeleccionadas());
        recortar.setOnClickListener(this);
        edicion.setOnClickListener(this);
        eliminar.setOnClickListener(this);
        regresar.setOnClickListener(this);
        guardar.setOnClickListener(this);
        camara.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);

        if (id == R.id.guardar) {
            if (listaSeleccionadaParaGuardar.isEmpty()) {
                Toast.makeText(requireContext(), "No hay fotos seleccionadas", Toast.LENGTH_SHORT).show();
                return;
            }
            ejecutarCifrado(nav);
        }
        else if (id == R.id.scann) {
            nav.navigate(R.id.escanerCifradoMixto);
        }
        else if (id == R.id.regresar) {
            limpiarArchivosTemporales();
            nav.navigate(R.id.escanerCifradoMixto);
        }
        else if (id == R.id.recortar || id == R.id.edicion || id == R.id.eliminar) {
            if (!listaSeleccionadaParaGuardar.isEmpty()) {
                sharedViewModel.setImageUriList(new ArrayList<>(listaSeleccionadaParaGuardar));
                int dest = (id == R.id.recortar) ? R.id.cortarRotar :
                        (id == R.id.edicion) ? R.id.visualizacionYReordenamiento : R.id.eliminarPaginas;
                nav.navigate(dest);
            }
        }
    }

    private void ejecutarCifrado(NavController nav) {
        guardar.setEnabled(false);
        final Context appCtx = requireContext().getApplicationContext();
        final CargaViewModel vm = this.cargaViewModel;

        // 1. Pasar el destino final para que la pantalla de carga sepa navegar al terminar
        Bundle args = new Bundle();
        args.putInt("destino_final", R.id.cifradoEscaneo2);
        nav.navigate(R.id.cargaProcesos, args);

        EscanerProcesador.generarPdfYEnviar(
                appCtx,
                new ArrayList<>(listaSeleccionadaParaGuardar),
                archivoService,
                archivoDAO,
                () -> { // ÉXITO
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        vm.terminarProceso(); // Dispara la animación de éxito y el cierre
                        borrarOriginalesYLimpiar();
                        Log.d(TAG, "Cifrado Mixto Exitoso");
                    }, 800);
                },
                errorMsg -> { // ERROR
                    new Handler(Looper.getMainLooper()).post(() -> {
                        vm.resetear();
                        nav.popBackStack();
                        guardar.setEnabled(true);
                        Toast.makeText(appCtx, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    });
                }
        );
    }

    private void guardarImagenesSeleccionadas() {
        if (adapter == null || adapter.getSelectedCount() == 0) return;

        List<Uri> selectedUris = adapter.getSelectedItems();
        int añadidos = 0;

        for (Uri galleryUri : selectedUris) {
            if (!listaOrigenesGaleria.contains(galleryUri)) {
                Uri copy = obtenerCopiaEditableUnica(galleryUri);
                if (copy != null) {
                    listaSeleccionadaParaGuardar.add(copy);
                    listaOrigenesGaleria.add(galleryUri);
                    añadidos++;
                }
            }
        }

        sharedViewModel.setImageUriList(new ArrayList<>(listaSeleccionadaParaGuardar));
        adapter.clearSelection();
        actualizarBarra();
        Toast.makeText(getContext(), añadidos + " nuevas imágenes añadidas", Toast.LENGTH_SHORT).show();
    }

    private Uri obtenerCopiaEditableUnica(Uri galleryUri) {
        try {
            String filename = "editable_" + UUID.randomUUID().toString() + ".jpg";
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

    private void borrarOriginalesYLimpiar() {
        ContentResolver resolver = requireContext().getContentResolver();
        for (Uri uri : listaOrigenesGaleria) {
            try { resolver.delete(uri, null, null); } catch (Exception ignored) {}
        }
        limpiarArchivosTemporales();
    }

    private void limpiarArchivosTemporales() {
        File cacheDir = requireContext().getCacheDir();
        File[] files = cacheDir.listFiles((dir, name) -> name.startsWith("editable_"));
        if (files != null) for (File f : files) f.delete();
        listaSeleccionadaParaGuardar.clear();
        listaOrigenesGaleria.clear();
        sharedViewModel.clearList();
    }

    private void pedirPermiso() {
        String p = (Build.VERSION.SDK_INT >= 33) ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED) cargarImagenes();
        else requestPermissions(new String[]{p}, REQUEST_CODE);
    }

    private void cargarImagenes() {
        listaMixta.clear();
        Uri coll = (Build.VERSION.SDK_INT >= 29) ? MediaStore.Images.Media.getContentUri("external") : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = requireActivity().getContentResolver().query(coll, new String[]{MediaStore.Images.Media._ID}, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) listaMixta.add(ContentUris.withAppendedId(coll, cursor.getLong(idCol)));
            }
        } catch (Exception e) { Log.e(TAG, "Error", e); }

        adapter = new ImageAdapter(listaMixta, new ImageAdapter.Callbacks() {
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