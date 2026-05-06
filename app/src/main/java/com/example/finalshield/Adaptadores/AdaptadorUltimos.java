package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.finalshield.Model.ArchivoMetadata;
import com.example.finalshield.R;

import java.util.List;

public class AdaptadorUltimos extends BaseAdapter {

    private Context context;
    private List<ArchivoMetadata> lista;

    public AdaptadorUltimos(Context context, List<ArchivoMetadata> lista) {
        this.context = context;
        this.lista = lista;
    }

    @Override
    public int getCount() {
        return lista.size();
    }

    @Override
    public ArchivoMetadata getItem(int position) {
        return lista.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.a_list_ultimos_archivos, parent, false);
        }

        ArchivoMetadata archivo = lista.get(position);
        RadioButton radioButton = convertView.findViewById(R.id.radio);
        TextView textView = convertView.findViewById(R.id.text);

        // Configuración de texto y estado
        textView.setText(archivo.getNombre());
        radioButton.setClickable(false);
        radioButton.setChecked(true);

        // Lógica de colores: Verde (#4CAF50) si está cifrado, Rojo (#F44336) si no
        if (archivo.isEstaCifrado()) {
            radioButton.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            radioButton.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
        }

        // --- ANIMACIÓN ---
        convertView.animate().cancel();
        convertView.setAlpha(0f);
        convertView.setTranslationY(120f);

        long delay = (long) position * 100L; // Delay basado en la posición para efecto cascada

        convertView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .start();

        return convertView;
    }
}