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
public class AdaptadorArchivos extends BaseAdapter {
    private final Context contexto;
    private final List<ArchivoMetadata> listaArchivos;
    private final LayoutInflater inflater;
    private final AdaptadorListener listener;

    public interface AdaptadorListener {
        void onBorrarClick(int position);
        void onCambiarEstadoClick(int position);
        void onItemClick(int position);
        void onDescifrarClick(int position);  // Nuevo para descifrar
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

        final ArchivoMetadata archivo = listaArchivos.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String fechaStr = archivo.getFechaSeleccion() != null ? sdf.format(archivo.getFechaSeleccion()) : "";

        String descripcion = archivo.getNombre() + "\n(" + archivo.getTamanioFormateado() + ") - " + fechaStr;
        holder.textDescrip.setText(descripcion);
        holder.textEstatus.setText(archivo.getEstadoTexto());

        // Ícono candado cerrado
        holder.btnCifradoMark.setImageResource(R.drawable.candadolist);  // Tu ícono original

        // Clic en candado → descifrar
        holder.btnCifradoMark.setOnClickListener(v -> listener.onDescifrarClick(position));

        // Clic en item completo → abrir cifrado
        convertView.setOnClickListener(v -> listener.onItemClick(position));

        // Borrar
        holder.btnBorrar.setOnClickListener(v -> listener.onBorrarClick(position));

        return convertView;
    }

    static class ViewHolder {
        TextView textDescrip;
        TextView textEstatus;
        ImageButton btnBorrar;
        ImageButton btnCifradoMark;
    }
}