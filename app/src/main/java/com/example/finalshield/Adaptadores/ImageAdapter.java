package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    public interface OnImageClickListener {
        void onImageClick(Uri uri);
    }

    private Context context;
    private List<Uri> listaImagenes;
    private Set<Uri> imagenesSeleccionadas = new HashSet<>();
    private boolean modoSeleccion = false;
    private OnImageClickListener listener;

    public ImageAdapter(Context context, List<Uri> listaImagenes, OnImageClickListener listener) {
        this.context = context;
        this.listaImagenes = listaImagenes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_imagen, parent, false);
        return new ImageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri uri = listaImagenes.get(position);

        holder.image.setImageURI(uri);

        // Efecto visual cuando está seleccionado
        holder.overlay.setVisibility(
                imagenesSeleccionadas.contains(uri) ? View.VISIBLE : View.GONE
        );

        // Click normal
        holder.itemView.setOnClickListener(v -> {
            if (modoSeleccion) {
                alternarSeleccion(uri);
                notifyItemChanged(position);
            } else {
                listener.onImageClick(uri);
            }
        });

        // Mantener presionado → activar selección múltiple
        holder.itemView.setOnLongClickListener(v -> {
            modoSeleccion = true;
            alternarSeleccion(uri);
            notifyItemChanged(position);
            return true;
        });
    }

    private void alternarSeleccion(Uri uri) {
        if (imagenesSeleccionadas.contains(uri)) {
            imagenesSeleccionadas.remove(uri);
        } else {
            imagenesSeleccionadas.add(uri);
        }
    }

    public Set<Uri> getSeleccionadas() {
        return imagenesSeleccionadas;
    }

    @Override
    public int getItemCount() {
        return listaImagenes.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        View overlay;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img);
            overlay = itemView.findViewById(R.id.overlay);
        }
    }
}
