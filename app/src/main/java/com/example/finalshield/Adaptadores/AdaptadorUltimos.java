package com.example.finalshield.Adaptadores;

import android.content.Context;
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
    public Object getItem(int position) {
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

        textView.setText(archivo.getNombre());

        radioButton.setClickable(false);
        radioButton.setChecked(true);

        if (archivo.isEstaCifrado()) {
            radioButton.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#4CAF50")));
        } else {
            radioButton.setButtonTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#F44336")));
        }

        // 🧹 LIMPIAR estado previo (MUY IMPORTANTE)
        convertView.animate().cancel();
        convertView.setAlpha(0f);
        convertView.setTranslationY(120f);

        // 🔥 ANIMACIÓN DE ABAJO HACIA ARRIBA
        int total = getCount();
        long delay = (total - position) * 70L;

        convertView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(delay)
                .start();

        return convertView;
    }
}