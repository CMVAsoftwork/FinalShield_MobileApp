package com.example.finalshield.Fragments.MenuPrincipal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.finalshield.Adaptadores.FaqAdapter;
import com.example.finalshield.Model.Faq;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.List;

public class ArchivosCifrados extends Fragment implements View.OnClickListener{
    ListView listac;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Navegar a inicio y limpiar la pila para evitar bucles
                Navigation.findNavController(requireView()).navigate(R.id.inicio, null,
                        new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archivos_cifrados, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        if (getActivity() != null) {
            //getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        List<Faq> faqs = new ArrayList<>();

// 🔐 Seguridad y cifrado
        faqs.add(new Faq(
                "¿Qué es FinalShield?",
                "FinalShield es una aplicación móvil que protege tus archivos y mensajes mediante cifrado, asegurando que solo las personas autorizadas puedan acceder a ellos.",
                R.drawable.escudo));

        faqs.add(new Faq(
                "¿Qué significa que un archivo esté cifrado?",
                "Significa que el archivo está protegido con una clave especial y no puede leerse sin autorización, incluso si alguien más lo obtiene.",
                R.drawable.candadoblan));

        faqs.add(new Faq(
                "¿Mis archivos están seguros en FinalShield?",
                "Sí. FinalShield utiliza métodos de cifrado seguros para proteger tu información y evitar accesos no autorizados.",
                R.drawable.archivoseguro));

// 📂 Archivos
        faqs.add(new Faq(
                "¿Cómo cifro un archivo desde la app?",
                "Solo selecciona el archivo desde la aplicación y elige la opción Cifrar. El proceso es rápido y automático.",
                R.drawable.carpetiux));

        faqs.add(new Faq(
                "¿Puedo descifrar un archivo en cualquier momento?",
                "Sí, siempre que estés autenticado y tengas permiso, puedes descifrar tus archivos cuando lo necesites.",
                R.drawable.candadoopen));

        faqs.add(new Faq(
                "¿Qué tipos de archivos puedo proteger?",
                "Puedes cifrar documentos, imágenes y otros archivos compatibles con tu dispositivo móvil.",
                R.drawable.circlepicture));

// 🔗 Enlaces seguros
        faqs.add(new Faq(
                "¿Qué es un enlace seguro?",
                "Es un enlace especial que permite compartir un archivo o mensaje cifrado de forma segura.",
                R.drawable.enlace));

        faqs.add(new Faq(
                "¿El enlace seguro tiene caducidad?",
                "Sí, los enlaces tienen una duración de 1 hora para mayor seguridad.",
                R.drawable.tiempo));

        faqs.add(new Faq(
                "¿Necesito una cuenta para abrir un enlace seguro?",
                "Sí, el destinatario debe autenticarse para poder acceder al contenido protegido.",
                R.drawable.person));

// 👤 Cuenta y acceso
        faqs.add(new Faq(
                "¿Necesito crear una cuenta para usar FinalShield?",
                "Sí, crear una cuenta permite proteger tu información y controlar el acceso a tus archivos.",
                R.drawable.cuenta2));

// ⚙️ Ayuda general
        faqs.add(new Faq(
                "¿La app consume muchos recursos del teléfono?",
                "No, FinalShield está optimizada para funcionar de manera eficiente en dispositivos móviles.",
                R.drawable.grafica));

        faqs.add(new Faq(
                "¿Dónde puedo obtener más ayuda?",
                "Puedes consultar esta sección o contactar al soporte por Whatsapp o llenando un formulario.",
                R.drawable.infoicon));


        ImageButton perfil,house, archivo,candadclose, carpeta, mail, candadopen;
        listac = v.findViewById(R.id.listacarp);
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
        // Dentro de onViewCreated
        v.findViewById(R.id.btnWhatsappIcon).setOnClickListener(view -> abrirWhatsApp());
        v.findViewById(R.id.btnFormulario).setOnClickListener(view -> abrirFormulario());
        CardView btnForm = v.findViewById(R.id.btnFormulario);

        FaqAdapter adapter = new FaqAdapter(requireContext(), faqs);
        listac.setAdapter(adapter);

    }

    private void abrirWhatsApp() {
        try {
            String numero = "525533539810";
            String mensaje = "Hola FinalShield, necesito soporte técnico.";
            String url = "https://api.whatsapp.com/send?phone=" + numero + "&text=" + Uri.encode(mensaje);

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            // Opcional: i.setPackage("com.whatsapp"); // Esto fuerza a abrir la app de WA
            startActivity(i);
        } catch (Exception e) {
            // Por si el usuario no tiene WhatsApp, lo mandamos al navegador
            Toast.makeText(getContext(), "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }
    // Método para abrir el Formulario de Google
    private void abrirFormulario() {
        String urlForm = "https://forms.gle/PWXtt7Eszxjmryj7A";
        try {
            Uri uri = Uri.parse(urlForm);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            // Esto ayuda a que el navegador lo abra como una pestaña nueva y limpia
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al abrir el formulario", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.carpeta){
            Navigation.findNavController(v).navigate(R.id.cifradoEscaneo2);
        } else if (v.getId() == R.id.house) {
            Navigation.findNavController(v).navigate(R.id.inicio, null,
                    new NavOptions.Builder().setPopUpTo(R.id.inicio, true).build());
        } else if (v.getId() == R.id.candadoclose) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados2);
        } else if (v.getId() == R.id.candadopen) {
            Navigation.findNavController(v).navigate(R.id.filtroDescifrados);
        } else if (v.getId() == R.id.mail) {
            Navigation.findNavController(v).navigate(R.id.servivioCorreo);
        }else if (v.getId() == R.id.archivo) {
            Navigation.findNavController(v).navigate(R.id.archivosCifrados);
        } else if (v.getId() == R.id.btnperfil) {
            Navigation.findNavController(v).navigate(R.id.perfil2);
        }
    }
}