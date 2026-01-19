package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdaptadorArchivos extends BaseAdapter {

    private final Context contexto;
    private final List<ArchivoMetadata> listaArchivos;
    private final LayoutInflater inflater;
    private final AdaptadorListener listener;
    private int lastPosition = -1;

    public interface AdaptadorListener {
        void onBorrarClick(int position);
        void onCambiarEstadoClick(int position);
        void onItemClick(int position);
        void onDescifrarClick(int position);
    }

    public AdaptadorArchivos(Context contexto, List<ArchivoMetadata> listaArchivos, AdaptadorListener listener) {
        this.contexto = contexto;
        this.listaArchivos = listaArchivos;
        this.inflater = LayoutInflater.from(contexto);
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return listaArchivos.size();
    }

    @Override
    public Object getItem(int i) {
        return listaArchivos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.archivos_list_cifrados, parent, false);
            holder = new ViewHolder();
            holder.textDescrip = convertView.findViewById(R.id.textdescrip);
            holder.textEstatus = convertView.findViewById(R.id.textestatus);
            holder.btnBorrar = convertView.findViewById(R.id.borrar);
            holder.btnCifradoMark = convertView.findViewById(R.id.descifrar);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ArchivoMetadata archivo = listaArchivos.get(position);

        // 1. Formatear Fecha (Sigue igual, está bien)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String fechaStr = archivo.getFechaSeleccion() != null ? sdf.format(archivo.getFechaSeleccion()) : "N/A";

        // 2. Determinar Ruta INTELIGENTE
        // Priorizamos la ruta de descifrado si el archivo ya no está cifrado
        String rutaAMostrar = "Pendiente...";

        if (!archivo.isEstaCifrado() && archivo.getRutaLocalDescifrado() != null) {
            rutaAMostrar = archivo.getRutaLocalDescifrado();
        } else if (archivo.getRutaLocalEncriptada() != null) {
            rutaAMostrar = archivo.getRutaLocalEncriptada();
        } else if (archivo.getRutaServidor() != null) {
            rutaAMostrar = archivo.getRutaServidor();
        }

        // 3. Construir String Multilínea
        // Usamos getTamanioFormateado() que ya tienes en el modelo
        String infoCompleta = "Archivo: " + archivo.getNombre() + "\n"
                + "Tamaño: " + archivo.getTamanioFormateado() + "\n"
                + "Fecha: " + fechaStr + "\n"
                + "Ruta: " + rutaAMostrar;

        holder.textDescrip.setText(infoCompleta);

        // --- ESTADO Y BOTONES ---
        if (archivo.isEstaCifrado()) {
            holder.textEstatus.setText("Cifrado");
            holder.textEstatus.setTextColor(ContextCompat.getColor(contexto, android.R.color.holo_red_dark));
            holder.btnCifradoMark.setImageResource(R.drawable.candadolist);
        } else {
            holder.textEstatus.setText("Descifrado");
            holder.textEstatus.setTextColor(ContextCompat.getColor(contexto, android.R.color.holo_green_dark));
            holder.btnCifradoMark.setImageResource(R.drawable.candadoopen);
        }

        holder.btnCifradoMark.setOnClickListener(v -> listener.onDescifrarClick(position));
        holder.btnBorrar.setOnClickListener(v -> listener.onBorrarClick(position));
        convertView.setOnClickListener(v -> listener.onItemClick(position));

        // Animación (Sin cambios)
        if (position > lastPosition) {
            convertView.setTranslationY(100f);
            convertView.setAlpha(0f);
            convertView.animate().translationY(0f).alpha(1f).setDuration(500).setStartDelay(position * 20L).start();
            lastPosition = position;
        }

        return convertView;
    }

    static class ViewHolder {
        TextView textDescrip;
        TextView textEstatus;
        ImageButton btnBorrar;
        ImageButton btnCifradoMark;
    }
}