package com.example.finalshield.Fragments.Correo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.finalshield.API.DescargaAPI;
import com.example.finalshield.DTO.Correo.ArchivoCorreoDTO;
import com.example.finalshield.DTO.Correo.CorreoDTO;
import com.example.finalshield.DTO.DescifradoRequest;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.Service.DescargaService;
import com.example.finalshield.Service.EnlaceService;
import com.example.finalshield.Util.CifradoUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerClave extends Fragment {

    private EditText etTokenSeguro;
    private TextView tvContenidoDescifrado, tvDialogMsg;
    private LinearLayout layoutAdjuntos, dialogContainer;
    private Button btnSi, btnNo, btnRegresar;
    private ProgressBar pbDescarga;

    private EnlaceService enlaceService;
    private AuthService authService;
    private DescargaAPI descargaAPI;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ver_clave, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Seguridad: Evitar capturas de pantalla
        if (getActivity() != null) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // 1. Vincular vistas
        etTokenSeguro = view.findViewById(R.id.etTokenSeguro);
        tvContenidoDescifrado = view.findViewById(R.id.tvContenidoDescifrado);
        layoutAdjuntos = view.findViewById(R.id.layoutAdjuntos);
        dialogContainer = view.findViewById(R.id.dialogContainer);
        btnSi = view.findViewById(R.id.sicifrar);
        btnNo = view.findViewById(R.id.nocifrar);
        btnRegresar = view.findViewById(R.id.regresarEnlace);
        tvDialogMsg = view.findViewById(R.id.tvDialogMsg);
        pbDescarga = view.findViewById(R.id.pbDescargaAdjuntos);

        // 2. Servicios
        enlaceService = new EnlaceService(requireContext());
        authService = new AuthService(requireContext());
        descargaAPI = new DescargaService(requireContext()).getAPI();

        // 3. Botones base
        btnRegresar.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.inicio));
        btnNo.setOnClickListener(v -> dialogContainer.setVisibility(View.GONE));

        // 4. Obtener Token y cargar datos
        recuperarTokenYCargar();
    }

    private void recuperarTokenYCargar() {
        String token = null;

        // Prioridad 1: Argumentos directos (lo que envió CargaProcesos)
        if (getArguments() != null) {
            token = getArguments().getString("security_token");
        }

        // Prioridad 2: SharedPreferences (si falló la navegación o abriste directo)
        if (token == null) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
            token = prefs.getString("pending_token", null);
        }

        if (token != null) {
            etTokenSeguro.setText(token);
            verContenido(token);
        } else {
            tvContenidoDescifrado.setText("Error: No se encontró un enlace válido para descifrar.");
        }
    }

    private void verContenido(String token) {
        String correo = authService.obtenerCorreo();
        enlaceService.getAPI().accederClave(token, correo).enqueue(new Callback<CorreoDTO>() {
            @Override
            public void onResponse(Call<CorreoDTO> call, Response<CorreoDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ÉXITO: Recién aquí borramos el token de las preferencias para que no se pierda si falla la red
                    requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE)
                            .edit().remove("pending_token").apply();

                    procesarDatosCorreo(response.body());
                } else {
                    tvContenidoDescifrado.setText("Acceso denegado, correo incorrecto o enlace caducado.");
                }
            }

            @Override
            public void onFailure(Call<CorreoDTO> call, Throwable t) {
                tvContenidoDescifrado.setText("Error de red al conectar con el servidor.");
            }
        });
    }

    private void procesarDatosCorreo(CorreoDTO correo) {
        try {
            // Descifrado usando la utilidad de FinalShield
            String descifrado = CifradoUtils.descifrarTexto(correo.getContenidoCifrado(), correo.getClaveCifDes());
            tvContenidoDescifrado.setText(descifrado);
            mostrarListaArchivos(correo.getArchivosAdjuntos(), correo.getClaveCifDes());
        } catch (Exception e) {
            tvContenidoDescifrado.setText("Error al descifrar el contenido.");
        }
    }

    private void mostrarListaArchivos(List<ArchivoCorreoDTO> adjuntos, String clave) {
        layoutAdjuntos.removeAllViews();
        if (adjuntos == null || adjuntos.isEmpty()) return;

        for (ArchivoCorreoDTO archivo : adjuntos) {
            Button btnArchivo = new Button(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 15, 0, 15);
            btnArchivo.setLayoutParams(params);

            btnArchivo.setBackgroundResource(R.drawable.bg_card_perfil);
            btnArchivo.setText("📁  " + archivo.getNombreOriginal());
            btnArchivo.setTextColor(Color.WHITE);
            btnArchivo.setAllCaps(false);
            btnArchivo.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            btnArchivo.setPadding(50, 40, 50, 40);

            btnArchivo.setOnClickListener(v -> {
                tvDialogMsg.setText("¿Deseas descargar:\n" + archivo.getNombreOriginal() + "?");
                dialogContainer.setVisibility(View.VISIBLE);

                btnSi.setOnClickListener(vSi -> {
                    dialogContainer.setVisibility(View.GONE);
                    iniciarDescarga(archivo, clave);
                });
            });

            layoutAdjuntos.addView(btnArchivo);
        }
    }

    private void iniciarDescarga(ArchivoCorreoDTO archivo, String clave) {
        pbDescarga.setVisibility(View.VISIBLE);
        descargaAPI.descifrarAdjunto(archivo.getIdArchivoCorreo(), new DescifradoRequest(clave))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        pbDescarga.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            if (guardarEnCarpetaDescargas(response.body(), archivo.getNombreOriginal())) {
                                Toast.makeText(getContext(), "Archivo guardado exitosamente", Toast.LENGTH_SHORT).show();
                                lanzarNotificacion(archivo.getNombreOriginal());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        pbDescarga.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error en la descarga", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean guardarEnCarpetaDescargas(ResponseBody body, String nombre) {
        try (InputStream in = body.byteStream()) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, nombre);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            }
            if (uri != null) {
                try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                    return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private void lanzarNotificacion(String nombre) {
        NotificationManager nm = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "fs_descargas";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(channelId, "Descargas", NotificationManager.IMPORTANCE_DEFAULT));
        }
        nm.notify(1, new NotificationCompat.Builder(requireContext(), channelId)
                .setSmallIcon(R.drawable.descarga)
                .setContentTitle("Descarga completada")
                .setContentText(nombre)
                .setAutoCancel(true).build());
    }
}