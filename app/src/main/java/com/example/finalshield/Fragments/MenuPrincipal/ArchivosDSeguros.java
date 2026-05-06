package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.example.finalshield.Adaptadores.AdaptadorArchivos;
import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;
import com.example.finalshield.Util.SecurityUtils;
import com.example.finalshield.ViewModel.CargaViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class ArchivosDSeguros extends Fragment implements View.OnClickListener, AdaptadorArchivos.AdaptadorListener {

    private ListView listView;
    private AdaptadorArchivos adaptador;
    private final List<ArchivoMetadata> listaMetadata = new ArrayList<>();
    private CargaViewModel cargaViewModel; // El motor de carga para el señuelo

    private LinearLayout dialogContainerCifrar;
    private LinearLayout dialogContainerEliminar;
    private int posicionSeleccionada = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializamos el ViewModel para controlar la carga falsa
        cargaViewModel = new ViewModelProvider(requireActivity()).get(CargaViewModel.class);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.inicio, null,
                        new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_d_seguros, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        listView = v.findViewById(R.id.listadesc);
        dialogContainerCifrar = v.findViewById(R.id.dialogContainer);
        dialogContainerEliminar = v.findViewById(R.id.dialogContainer2);

        adaptador = new AdaptadorArchivos(getContext(), listaMetadata, this);
        listView.setAdapter(adaptador);

        v.findViewById(R.id.sicifrar).setOnClickListener(view -> ejecutarAccionFalsaCifrar());
        v.findViewById(R.id.nocifrar).setOnClickListener(view -> ocultarDialogos());

        v.findViewById(R.id.sieliminar2).setOnClickListener(view -> ejecutarAccionFalsaEliminar());
        v.findViewById(R.id.noeliminar2).setOnClickListener(view -> ocultarDialogos());

        int[] navIds = {R.id.house, R.id.candadoclose, R.id.carpeta, R.id.archivo, R.id.mail, R.id.btnperfil};
        for (int id : navIds) {
            View btn = v.findViewById(id);
            if (btn != null) btn.setOnClickListener(this);
        }

        generarBovedaSenuelo();
    }

    private void generarBovedaSenuelo() {
        listaMetadata.clear();
        Random random = new Random();

        // Plantillas de contenido denso y creíble
        String seedPhrase = "SEED PHRASE RECOVERY - GENERATED 2024-11-12\n\n" +
                "1. ocean  2. master  3. velvet  4. logic\n5. cluster 6. budget 7. picnic 8. loyalty\n" +
                "9. verify 10. adjust 11. casual 12. merit\n\n" +
                "WARNING: Store this offline. Do not take screenshots.";

        String dbConfig = "### DATABASE CONFIGURATION - PRODUCTION ENVIRONMENT ###\n" +
                "DB_HOST=192.168.1.104\nDB_PORT=5432\nDB_NAME=finalshield_prod_db\n" +
                "DB_USER=admin_fs\nDB_PASS=S3cur3_P@ss_2026_!\n" +
                "MAX_CONNECTIONS=100\nSSL_MODE=require\n\n# Last maintenance: 2026-01-15";

        String bankingLog = "MOVIMIENTOS RECIENTES - TARJETA **** 9921\n" +
                "-------------------------------------------\n" +
                "02/03/26 - SPEI RECIBIDO - $8,400.00 - NOMINA\n" +
                "03/03/26 - PAGO OXXO - $154.20 - SERVICIOS\n" +
                "04/03/26 - RETIRO CAJERO - $2,000.00 - ATM_MEX_02\n" +
                "SALDO DISPONIBLE: $12,456.80 MXN";

        Object[][] datosFalsos = {
                {"Crypto_Seed_Vault.txt", "CACHE", seedPhrase},
                {"Production_DB_Config.conf", "CACHE", dbConfig},
                {"Resumen_Bancario_Marzo.txt", "CACHE", bankingLog},
                {"Admin_Access_Keys.txt", "CACHE", "AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE\nAWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"},
                {"Plan_Desarrollo_Q3.pdf", "ESCANEO", "CONFIDENCIAL: Estrategia de expansión de CMVA Softwork para el cierre de año."},
                {"Backdoor_Notes.txt", "CACHE", "Si el servidor principal falla, usar el tunel SSH en el puerto 2202."},
        };

        for (Object[] item : datosFalsos) {
            ArchivoMetadata m = new ArchivoMetadata();
            m.setNombre((String) item[0]);
            m.setOrigen((String) item[1]);
            m.setEstaCifrado(false);
            m.setFechaSeleccion(new Date());

            // Simular un tamaño de archivo más real (entre 15KB y 80KB)
            m.setTamanioBytes(15000 + random.nextInt(65000));

            File dummyFile = new File(requireContext().getCacheDir(), (String) item[0]);
            try (FileOutputStream fos = new FileOutputStream(dummyFile)) {
                // Repetimos el contenido o agregamos basura para que el archivo pese
                String contenido = (String) item[2];
                fos.write(contenido.getBytes());
                // Agregamos 1KB de "ruido" invisible para que el sistema reporte un tamaño mayor
                fos.write(new byte[1024]);
            } catch (Exception ignored) {}

            m.setRutaLocalDescifrado(dummyFile.getAbsolutePath());
            listaMetadata.add(m);
        }
        adaptador.notifyDataSetChanged();
    }
    private void ejecutarAccionFalsaCifrar() {
        if (posicionSeleccionada == -1) return;
        ocultarDialogos();

        // 1. Ir a carga (destino falso: Archivos Cifrados reales)
        Bundle args = new Bundle();
        args.putInt("destino_final", R.id.archivosCifrados2);
        Navigation.findNavController(requireView()).navigate(R.id.cargaProcesos, args);

        // 2. Simular tiempo de procesamiento "pesado" para dar realismo
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded()) {
                cargaViewModel.terminarProceso();
                Toast.makeText(getContext(), "Archivo protegido exitosamente", Toast.LENGTH_SHORT).show();
            }
        }, 1200);
    }

    private void ejecutarAccionFalsaEliminar() {
        if (posicionSeleccionada == -1) return;

        ArchivoMetadata archivo = listaMetadata.get(posicionSeleccionada);
        ocultarDialogos();

        // TRITURAMOS el archivo falso para que si el atacante intenta
        // recuperarlo con herramientas forenses, ¡vea que la app "funciona" de verdad!
        if (archivo.getRutaLocalDescifrado() != null) {
            SecurityUtils.borrarPermanente(new File(archivo.getRutaLocalDescifrado()));
        }

        listaMetadata.remove(posicionSeleccionada);
        adaptador.notifyDataSetChanged();
        Toast.makeText(getContext(), "Archivo triturado y eliminado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(int position) {
        ArchivoMetadata archivo = listaMetadata.get(position);
        File file = new File(archivo.getRutaLocalDescifrado());

        try {
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "text/plain");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Ver archivo:"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: Acceso denegado por el sistema", Toast.LENGTH_SHORT).show();
        }
    }

    private void ocultarDialogos() {
        if (dialogContainerCifrar != null) dialogContainerCifrar.setVisibility(View.GONE);
        if (dialogContainerEliminar != null) dialogContainerEliminar.setVisibility(View.GONE);
        posicionSeleccionada = -1;
    }

    @Override public void onDescifrarClick(int position) { this.posicionSeleccionada = position; dialogContainerCifrar.setVisibility(View.VISIBLE); }
    @Override public void onBorrarClick(int position) { this.posicionSeleccionada = position; dialogContainerEliminar.setVisibility(View.VISIBLE); }
    @Override public void onCambiarEstadoClick(int position) {}

    @Override
    public void onClick(View v) {
        int id = v.getId();
        NavController nav = Navigation.findNavController(v);

        if (id == R.id.house) {
            nav.navigate(R.id.inicio, null, new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        } else if (id == R.id.candadoclose) {
            nav.navigate(R.id.archivosCifrados2);
        } else if (id == R.id.carpeta) {
            nav.navigate(R.id.cifradoEscaneo2);
        } else if (id == R.id.archivo) {
            nav.navigate(R.id.archivosCifrados);
        } else if (id == R.id.mail) {
            nav.navigate(R.id.servivioCorreo);
        } else if (id == R.id.btnperfil) {
            nav.navigate(R.id.perfil2);
        }
    }
}