package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.finalshield.R;

public class Adaptador4 extends BaseAdapter {
    Context contexto;
    String listadeportes[];
    String listacomentar[];
    int listaimagenes[];
    LayoutInflater inflater;

    public Adaptador4(Context context, String[] listadeportes, String[] listacomentar, int[] listaimagenes) {
        this.contexto = context;
        this.listadeportes = listadeportes;
        this.listacomentar = listacomentar;
        this.listaimagenes = listaimagenes;
        this.inflater = LayoutInflater.from(contexto);
    }
    @Override
    public int getCount() {return listacomentar.length;}
    @Override
    public Object getItem(int i) {
        return null;
    }
    @Override
    public long getItemId(int i) {
        return 0;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.fragment_registro_sesion,null);
        ImageView img;
        TextView text1, text2;
        //text1 = convertView.    findViewById(R.id.text1);
        //text2 = convertView.findViewById(R.id.text2);
        //img = convertView.findViewById(R.id.img);
        //text1.setText(listadeportes[position]);
        //text2.setText(listacomentar[position]);
        //img.setImageResource(listaimagenes[position]);
        return convertView;
    }
}
