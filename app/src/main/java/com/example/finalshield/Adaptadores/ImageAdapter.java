package com.example.finalshield.Adaptadores;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.finalshield.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private final List<Uri> lista;
    private final Callbacks listener;
    private final Set<Uri> seleccionadas = new HashSet<>();
    private boolean selectionMode = false;
    private int selectedIndex = RecyclerView.NO_POSITION;
    private final @LayoutRes int layoutResId;
    private final boolean longClickSelectionEnabled;

    public interface Callbacks {
        void onImageClicked(Uri uri);
        void onSelectionChanged(int count);
    }

    public ImageAdapter(List<Uri> lista, Callbacks listener) {
        this(lista, listener, R.layout.item_imagen, false);
    }

    public ImageAdapter(List<Uri> lista, Callbacks listener, @LayoutRes int layoutResId) {
        this(lista, listener, layoutResId, false);
    }

    public ImageAdapter(List<Uri> lista, Callbacks listener, @LayoutRes int layoutResId, boolean longClickSelectionEnabled) {
        this.lista = lista;
        this.listener = listener;
        this.layoutResId = layoutResId;
        this.longClickSelectionEnabled = longClickSelectionEnabled;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(this.layoutResId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Uri uri = lista.get(pos);

        // Forzar recarga: no cache (esto hace que al volver de editar se vea la versión nueva)
        Glide.with(h.itemView.getContext())
                .load(uri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .override(300, 300)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_error_loading)
                .into(h.img);

        boolean isSelectedMultiple = seleccionadas.contains(uri);
        h.overlay.setVisibility(isSelectedMultiple ? View.VISIBLE : View.GONE);
        h.check.setVisibility(isSelectedMultiple ? View.VISIBLE : View.GONE);

        if (pos == selectedIndex && !selectionMode) {
            h.itemView.setBackgroundResource(R.drawable.border_selected_focus);
        } else {
            h.itemView.setBackground(null);
        }

        h.itemView.setOnClickListener(v -> {
            if (!selectionMode) {
                listener.onImageClicked(uri);
                return;
            }
            toggleSelection(uri);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (longClickSelectionEnabled && !selectionMode) {
                selectionMode = true;
                toggleSelection(uri);
                return true;
            }
            return false;
        });
    }

    public void setSelectedIndex(int index) {
        int oldIndex = this.selectedIndex;
        this.selectedIndex = index;

        if (oldIndex != RecyclerView.NO_POSITION) notifyItemChanged(oldIndex);
        if (index != RecyclerView.NO_POSITION) notifyItemChanged(index);
    }

    public void moverItemEficiente(int fromPosition, int toPosition) {
        Uri itemMovido = lista.remove(fromPosition);
        lista.add(toPosition, itemMovido);
        notifyItemMoved(fromPosition, toPosition);

        if (selectedIndex == fromPosition) {
            selectedIndex = toPosition;
        } else if (selectedIndex == toPosition) {
            selectedIndex = fromPosition;
        }
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public void toggleSelection(Uri uri) {
        if (seleccionadas.contains(uri)) seleccionadas.remove(uri); else seleccionadas.add(uri);
        listener.onSelectionChanged(seleccionadas.size());
        notifyDataSetChanged();
        if (seleccionadas.size() == 0) selectionMode = false;
    }

    public boolean isSelectionMode() { return selectionMode; }
    public int getSelectedCount() { return seleccionadas.size(); }

    public void clearSelection() {
        seleccionadas.clear();
        selectionMode = false;
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
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

    public List<Uri> getSelectedItems() { return new ArrayList<>(seleccionadas); }

    public List<Uri> getRetainedItems() {
        List<Uri> retained = new ArrayList<>(lista);
        if (seleccionadas.size() > 0) retained.removeAll(seleccionadas);
        return retained;
    }

    public List<Uri> discardSelectedItems() {
        List<Uri> discardedUris = getSelectedItems();
        lista.removeAll(discardedUris);
        seleccionadas.clear();
        selectionMode = false;
        selectedIndex = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
        listener.onSelectionChanged(0);
        return discardedUris;
    }
}
