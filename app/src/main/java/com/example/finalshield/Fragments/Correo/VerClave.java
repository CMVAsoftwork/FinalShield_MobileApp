package com.example.finalshield.Fragments.Correo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalshield.API.DescargaAPI;
import com.example.finalshield.DTO.Correo.ArchivoCorreoDTO;
import com.example.finalshield.DTO.Correo.CorreoDTO;
import com.example.finalshield.DTO.DescifradoRequest;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.Service.DescargaService;
import com.example.finalshield.Service.EnlaceService;
import com.example.finalshield.Util.CifradoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerClave extends Fragment implements View.OnClickListener {
    private EditText etTokenSeguro;
    private Button btnVerContenido;
    private TextView tvContenidoDescifrado;
    private LinearLayout layoutAdjuntos;
    private ProgressBar pbDescargaAdjuntos;

    private EnlaceService enlaceService;
    private AuthService authService;
    private DescargaAPI descargaAPI;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ver_clave, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etTokenSeguro = view.findViewById(R.id.etTokenSeguro);
        btnVerContenido = view.findViewById(R.id.btnVerContenido);
        tvContenidoDescifrado = view.findViewById(R.id.tvContenidoDescifrado);
        layoutAdjuntos = view.findViewById(R.id.layoutAdjuntos);
        pbDescargaAdjuntos = view.findViewById(R.id.pbDescargaAdjuntos);

        enlaceService = new EnlaceService(requireContext());
        authService = new AuthService(requireContext());
        descargaAPI = new DescargaService(requireContext()).getAPI();

        Button btnRegresar = view.findViewById(R.id.regresarEnlace);
        btnRegresar.setOnClickListener(v2 -> {
            Navigation.findNavController(v2).navigate(R.id.inicio);
        });

        if (getArguments() != null) {
            String token = getArguments().getString("security_token");

            if (token != null) {
                etTokenSeguro.setText(token);

                if (authService.obtenerCorreo() == null) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("deep_link", Context.MODE_PRIVATE);
                    prefs.edit().putString("pending_token", token).apply();

                    Toast.makeText(requireContext(), "Sesión requerida para descifrar.", Toast.LENGTH_SHORT).show();

                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.verClave, true)
                            .build();

                    Navigation.findNavController(view).navigate(R.id.inicioSesion, null, navOptions);
                    return;
                } else {
                    verContenido(token);
                }
            }
        }
        btnVerContenido.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String tokenCompleto = etTokenSeguro.getText().toString().trim();
        String token = extraerTokenDeUrl(tokenCompleto);
        if (!token.isEmpty()) {
            verContenido(token);
        } else {
            Toast.makeText(requireContext(), "Por favor, ingresa o pega un token/enlace válido.", Toast.LENGTH_SHORT).show();
        }
    }

    private String extraerTokenDeUrl(String tokenOUrl) {
        if (tokenOUrl == null) return "";

        if (tokenOUrl.contains("/api/enlaces/")) {
            Uri uri = Uri.parse(tokenOUrl);
            List<String> segments = uri.getPathSegments();
            int index = segments.indexOf("validar");
            if (index > 0) {
                return segments.get(index - 1);
            }
        }

        if (tokenOUrl.startsWith("fileshield://")) {
            Uri uri = Uri.parse(tokenOUrl);
            return uri.getQueryParameter("security_token");
        }

        return tokenOUrl;
    }

    private void verContenido(String token) {
        String correoUsuario = authService.obtenerCorreo();

        if (correoUsuario == null) {
            Toast.makeText(requireContext(), "Error: No hay una sesión activa. Por favor, inicia sesión.", Toast.LENGTH_LONG).show();
            return;
        }

        tvContenidoDescifrado.setText("Validando enlace y cargando contenido cifrado...");
        layoutAdjuntos.removeAllViews();
        pbDescargaAdjuntos.setVisibility(View.GONE);

        enlaceService.getAPI().accederClave(token, correoUsuario).enqueue(new Callback<CorreoDTO>() {
            @Override
            public void onResponse(Call<CorreoDTO> call, Response<CorreoDTO> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CorreoDTO correo = response.body();
                    procesarCorreo(correo);
                } else {
                    tvContenidoDescifrado.setText("Error: Token inválido, expirado o acceso no autorizado.");
                    Toast.makeText(requireContext(), "Error de API: Verifica tu token y sesión (HTTP " + response.code() + ").", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<CorreoDTO> call, Throwable t) {
                tvContenidoDescifrado.setText("Error de red: " + t.getMessage());
                Toast.makeText(requireContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void procesarCorreo(CorreoDTO correo) {
        try {
            String claveBase64 = correo.getClaveCifDes();
            String contenidoCifrado = correo.getContenidoCifrado();

            if (claveBase64 == null || contenidoCifrado == null) {
                tvContenidoDescifrado.setText("Error: Clave o Contenido Cifrado no recibidos.");
                return;
            }

            String contenidoDescifrado = CifradoUtils.descifrarTexto(contenidoCifrado, claveBase64);
            tvContenidoDescifrado.setText(contenidoDescifrado);

            listarAdjuntos(correo.getArchivosAdjuntos(), claveBase64);
            Toast.makeText(requireContext(), "Correo descifrado con éxito.", Toast.LENGTH_SHORT).show();

        } catch (GeneralSecurityException e) {
            tvContenidoDescifrado.setText("Error de seguridad al descifrar. La clave es incorrecta o el formato es inválido.");
            e.printStackTrace();
        } catch (Exception e) {
            tvContenidoDescifrado.setText("Error interno al procesar el mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void listarAdjuntos(List<ArchivoCorreoDTO> adjuntos, String claveBase64) {
        if (adjuntos == null || adjuntos.isEmpty()) {
            layoutAdjuntos.addView(crearTextView("No hay archivos adjuntos."));
            return;
        }

        for (ArchivoCorreoDTO archivo : adjuntos) {
            TextView tv = crearTextView("Archivo: " + archivo.getNombreOriginal() + " (Click para descargar)");
            tv.setOnClickListener(v -> iniciarDescargaAdjunto(archivo, claveBase64));
            tv.setBackgroundColor(getResources().getColor(R.color.degradado_azul_medio));
            tv.setTextColor(getResources().getColor(android.R.color.white));
            tv.setPadding(10, 10, 10, 10);
            layoutAdjuntos.addView(tv);
        }
    }

    private TextView crearTextView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private void iniciarDescargaAdjunto(ArchivoCorreoDTO archivo, String claveBase64) {
        Toast.makeText(requireContext(), "Solicitando descarga y descifrado de: " + archivo.getNombreOriginal(), Toast.LENGTH_LONG).show();
        pbDescargaAdjuntos.setVisibility(View.VISIBLE);

        DescifradoRequest request = new DescifradoRequest(claveBase64);

        descargaAPI.descifrarAdjunto(archivo.getIdArchivoCorreo(), request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                pbDescargaAdjuntos.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    boolean guardado = guardarArchivoEnAlmacenamiento(response.body(), archivo.getNombreOriginal());

                    if (guardado) {
                        Toast.makeText(requireContext(), "Descarga exitosa: " + archivo.getNombreOriginal(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Error al guardar el archivo. Verifica permisos.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(requireContext(), "Error en el servidor al descifrar el adjunto (HTTP " + response.code() + ").", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                pbDescargaAdjuntos.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error de red al descargar el adjunto: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean guardarArchivoEnAlmacenamiento(ResponseBody body, String fileName) {
        try (InputStream inputStream = body.byteStream()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = requireContext().getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

                if (uri != null) {
                    try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                        if (outputStream != null) {
                            return escribirStream(inputStream, outputStream);
                        }
                    }
                }
                return false;

            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                File outputFile = new File(downloadsDir, fileName);

                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    return escribirStream(inputStream, outputStream);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean escribirStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        return true;
    }
}