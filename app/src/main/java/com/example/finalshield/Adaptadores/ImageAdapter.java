package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalshield.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<Uri> lista;
    private final Callbacks listener;

    private final Set<Uri> seleccionadas = new HashSet<>();
    private boolean selectionMode = false;

    public interface Callbacks {
        void onImageClicked(Uri uri);
        void onSelectionChanged(int count);
    }

    public ImageAdapter(List<Uri> lista, Callbacks listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagen, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Uri uri = lista.get(pos);

        h.img.setImageURI(uri);

        boolean isSelected = seleccionadas.contains(uri);

        h.overlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        h.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {

            if (!selectionMode) {
                listener.onImageClicked(uri);
                return;
            }

            toggleSelection(uri);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                toggleSelection(uri);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void toggleSelection(Uri uri) {
        if (seleccionadas.contains(uri)) {
            seleccionadas.remove(uri);
        } else {
            seleccionadas.add(uri);
        }

        listener.onSelectionChanged(seleccionadas.size());
        notifyDataSetChanged();

        if (seleccionadas.size() == 0) {
            selectionMode = false;
        }
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getSelectedCount() {
        return seleccionadas.size();
    }

    public void clearSelection() {
        seleccionadas.clear();
        selectionMode = false;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, check;
        View overlay;

        public ViewHolder(@NonNull View v) {
            super(v);
            img = v.findViewById(R.id.img);
            overlay = v.findViewById(R.id.overlay);
            check = v.findViewById(R.id.checkIcon);
        }
    }
}
