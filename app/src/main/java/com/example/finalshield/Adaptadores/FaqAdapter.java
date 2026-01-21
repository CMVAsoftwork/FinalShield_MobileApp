package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.finalshield.Model.Faq;
import com.example.finalshield.R;

import java.util.List;

public class FaqAdapter extends BaseAdapter {
    private Context context;
    private List<Faq> lista;
    private int expandedPosition = -1;

    public FaqAdapter(Context context, List<Faq> lista) {
        this.context = context;
        this.lista = lista;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public Object getItem(int i) {
        return lista.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_pregunta_faq, parent, false);
        }

        // 1. Referencias
        TextView pregunta = convertView.findViewById(R.id.txtPregunta);
        TextView respuesta = convertView.findViewById(R.id.txtRespuesta);
        ImageView iconExpand = convertView.findViewById(R.id.iconExpand);
        ImageView imgCategoria = convertView.findViewById(R.id.imgCategoria);

        // 2. Datos
        Faq faq = lista.get(position);
        pregunta.setText(faq.getPregunta());
        respuesta.setText(faq.getRespuesta());
        imgCategoria.setImageResource(faq.getIcono());

        // 3. Lógica de expansión
        boolean isExpanded = position == expandedPosition;

        // Aplicar visibilidad de la respuesta
        respuesta.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Animación de la flecha
        iconExpand.animate().rotation(isExpanded ? 180 : 0).setDuration(200).start();

        // 4. Desactivar clics en hijos para que NO bloqueen el clic de la fila completa
        pregunta.setClickable(false);
        pregunta.setFocusable(false);
        imgCategoria.setClickable(false);
        imgCategoria.setFocusable(false);
        iconExpand.setClickable(false);
        iconExpand.setFocusable(false);

        // 5. AQUÍ ESTÁ EL LISTENER (El que hace la magia)
        convertView.setOnClickListener(v -> {
            if (expandedPosition == position) {
                expandedPosition = -1; // Si ya estaba abierta, la cierra
            } else {
                expandedPosition = position; // Si estaba cerrada, la abre
            }
            notifyDataSetChanged(); // Refresca la lista para mostrar el cambio
        });

        return convertView;
    }
}
