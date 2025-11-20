package com.example.finalshield.Fragments.Correo;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.finalshield.DTO.Correo.CorreoRequest;
import com.example.finalshield.R;
import com.example.finalshield.Service.AuthService;
import com.example.finalshield.Service.CorreoService;
import com.example.finalshield.Util.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServivioCorreo extends Fragment implements View.OnClickListener {
    private EditText etDestinatario;
    private EditText etAsunto;
    private EditText etMensaje;
    private Button btnAdjuntar;
    private Button btnEnviar;
    private CorreoService correoService;
    private AuthService authService;
    private final List<Uri> adjuntosUri = new ArrayList<>();
    private ActivityResultLauncher<String[]> filePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_servivio_correo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        correoService = new CorreoService(requireContext());
        authService = new AuthService(requireContext());

        etDestinatario = v.findViewById(R.id.paracontend);
        etAsunto = v.findViewById(R.id.asuntocontend);
        etMensaje = v.findViewById(R.id.mensajecontent);
        btnAdjuntar = v.findViewById(R.id.sieliminar);
        btnEnviar = v.findViewById(R.id.noeliminar);

        btnAdjuntar.setOnClickListener(v1 -> seleccionarArchivos());
        btnEnviar.setOnClickListener(v1 -> enviarCorreoCifrado());
        configurarFilePicker();

        ImageButton perfil, house, archivo, candadclose, carpeta, mail, candadopen;
        perfil = v.findViewById(R.id.btnperfil);
        house = v.findViewById(R.id.house);
        archivo = v.findViewById(R.id.archivo);
        candadclose = v.findViewById(R.id.candadoclose);
        carpeta = v.findViewById(R.id.carpeta);
        mail = v.findViewById(R.id.mail);
        candadopen = v.findViewById(R.id.candadopen);

        perfil.setOnClickListener(this);
        house.setOnClickListener(this);
        archivo.setOnClickListener(this);
        candadclose.setOnClickListener(this);
        carpeta.setOnClickListener(this);
        mail.setOnClickListener(this);
        candadopen.setOnClickListener(this);
    }

    private void configurarFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        adjuntosUri.clear();
                        adjuntosUri.addAll(uris);
                        Toast.makeText(requireContext(),
                                adjuntosUri.size() + " archivo(s) seleccionado(s).",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "No se seleccionaron archivos.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void seleccionarArchivos() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    private void enviarCorreoCifrado() {
        String destinatario = etDestinatario.getText().toString().trim();
        String asunto = etAsunto.getText().toString().trim();
        String mensaje = etMensaje.getText().toString().trim();

        if (destinatario.isEmpty() || asunto.isEmpty() || mensaje.isEmpty()) {
            Toast.makeText(requireContext(), "Todos los campos deben estar completos.", Toast.LENGTH_LONG).show();
            return;
        }

        CorreoRequest correoRequest = new CorreoRequest(asunto, mensaje, destinatario);

        RequestBody correoJsonPart = RequestBody.create(
                okhttp3.MediaType.parse("application/json"),
                correoService.getGson().toJson(correoRequest)
        );

        List<MultipartBody.Part> adjuntosPart = new ArrayList<>();

        for (Uri uri : adjuntosUri) {
            MultipartBody.Part filePart = FileUtils.prepareFilePart(requireContext(), uri);
            if (filePart != null) {
                adjuntosPart.add(filePart);
            }
        }

        correoService.getAPI().enviarCorreo(correoJsonPart,adjuntosPart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    etAsunto.setText("");
                    etMensaje.setText("");
                    etDestinatario.setText("");
                    adjuntosUri.clear();
                    Toast.makeText(requireContext(), "Correo cifrado enviado con éxito.", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Toast.makeText(requireContext(), "Error al enviar: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(requireContext(), "Error HTTP " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.carpeta) {
            Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        } else if (v.getId() == R.id.house) {
            Navigation.findNavController(v).navigate(R.id.continuacionInicio);
        } else if (v.getId() == R.id.candadoclose) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        } else if (v.getId() == R.id.candadopen) {
            Navigation.findNavController(v).navigate(R.id.continuacionInicio);
        } else if (v.getId() == R.id.mail) {
            Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        } else if (v.getId() == R.id.archivo) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        } else if (v.getId() == R.id.btnperfil) {

        }
    }
}