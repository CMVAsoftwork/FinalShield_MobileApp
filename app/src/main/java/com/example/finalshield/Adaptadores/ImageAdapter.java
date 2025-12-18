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

    // Bandera para permitir que la pulsación larga inicie la selección (solo para VerFotosTomadas)
    private final boolean longClickSelectionEnabled;

    public interface Callbacks {
        void onImageClicked(Uri uri);
        void onSelectionChanged(int count);
    }

    /**
     * Constructor 1 (2 argumentos): Usado por Reordenar.
     * Asume layout por defecto (item_imagen) y selección deshabilitada.
     */
    public ImageAdapter(List<Uri> lista, Callbacks listener) {
        this(lista, listener, R.layout.item_imagen, false);
    }

    /**
     * Constructor 2 (3 argumentos): Usado por Cortar/Rotar (Layout Delgado, sin selección).
     */
    public ImageAdapter(List<Uri> lista, Callbacks listener, @LayoutRes int layoutResId) {
        this(lista, listener, layoutResId, false);
    }

    /**
     * Constructor 3 (4 argumentos): Usado por VerFotosTomadas (Layout por defecto, con selección).
     */
    public ImageAdapter(List<Uri> lista, Callbacks listener, @LayoutRes int layoutResId, boolean longClickSelectionEnabled) {
        this.lista = lista;
        this.listener = listener;
        this.layoutResId = layoutResId;
        this.longClickSelectionEnabled = longClickSelectionEnabled;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(this.layoutResId, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Uri uri = lista.get(pos);

        // Carga de imagen
        Glide.with(h.itemView.getContext())
                .load(uri)
                // Opciones para forzar la recarga:
                // 1. Saltamos la caché de memoria para que no use la miniatura antigua.
                .skipMemoryCache(true)
                // 2. Saltamos la caché de disco para asegurar que se lee el archivo sobrescrito.
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(h.img);

        // Manejo del MODO SELECCIÓN MÚLTIPLE (Visual)
        boolean isSelectedMultiple = seleccionadas.contains(uri);
        h.overlay.setVisibility(isSelectedMultiple ? View.VISIBLE : View.GONE);
        h.check.setVisibility(isSelectedMultiple ? View.VISIBLE : View.GONE);

        // Manejo del MODO SELECCIÓN SIMPLE/ENFOQUE (Visual para CortarRotar)
        if (pos == selectedIndex && !selectionMode) {
            h.itemView.setBackgroundResource(R.drawable.border_selected_focus);
        } else {
            h.itemView.setBackground(null);
        }

        // Listeners
        h.itemView.setOnClickListener(v -> {
            if (!selectionMode) {
                // Click normal: Lanza la actividad/edición
                listener.onImageClicked(uri);
                return;
            }
            toggleSelection(uri);
        });

        h.itemView.setOnLongClickListener(v -> {
            // *** LÓGICA DE SELECCIÓN CONTROLADA ***
            if (longClickSelectionEnabled && !selectionMode) {
                selectionMode = true;
                toggleSelection(uri);
                return true; // Consumir el evento para iniciar selección
            }
            // Devolver false: permite el uso normal o deshabilita la selección
            return false;
        });
    }

    /**
     * Establece el índice de la imagen enfocada para el modo simple (Ej. Cortar/Rotar).
     */
    public void setSelectedIndex(int index) {
        int oldIndex = this.selectedIndex;
        this.selectedIndex = index;

        if (oldIndex != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldIndex);
        }
        if (index != RecyclerView.NO_POSITION) {
            notifyItemChanged(index);
        }
    }

    /**
     * Método requerido para ItemTouchHelper (Reordenamiento).
     */
    public void moverItemEficiente(int fromPosition, int toPosition) {
        Uri itemMovido = lista.remove(fromPosition);
        lista.add(toPosition, itemMovido);
        notifyItemMoved(fromPosition, toPosition);

        // Actualizar el índice de enfoque si se mueve la imagen seleccionada
        if (selectedIndex == fromPosition) {
            selectedIndex = toPosition;
        } else if (selectedIndex == toPosition) {
            selectedIndex = fromPosition;
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public void toggleSelection(Uri uri) {
        // 1. Encontrar la posición
        int position = lista.indexOf(uri);
        if (position == RecyclerView.NO_POSITION) return;

        // 2. Cambiar el estado de selección
        if (seleccionadas.contains(uri)) {
            seleccionadas.remove(uri);
        } else {
            seleccionadas.add(uri);
        }

        listener.onSelectionChanged(seleccionadas.size());

        // 3. ¡ACTUALIZAR SOLO EL ÍTEM ESPECÍFICO!
        // Esto evita el parpadeo general.
        notifyItemChanged(position);

        if (seleccionadas.size() == 0) {
            selectionMode = false;
            // Si al deseleccionar el último elemento quieres asegurar que el modo selección
            // se desactiva visualmente en el resto (por si hay algún borde/efecto global),
            // podrías considerar notificar cambios en los elementos visibles, pero generalmente
            // con 'notifyItemChanged' es suficiente si tu 'onBindViewHolder' maneja bien el 'selectionMode'.
        }
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public int getSelectedCount() {
        return seleccionadas.size();
    }

    public void clearSelection() {

        // 1. Obtener una lista de las posiciones a limpiar ANTES de borrar el 'Set'
        List<Integer> positionsToClear = new ArrayList<>();
        for (Uri uri : seleccionadas) {
            int position = lista.indexOf(uri);
            if (position != RecyclerView.NO_POSITION) {
                positionsToClear.add(position);
            }
        }

        // 2. Limpiar el estado
        seleccionadas.clear();
        selectionMode = false;

        // 3. Notificar los cambios de manera eficiente
        // Solo se actualizan las celdas que tenían un estado de selección.
        for (int position : positionsToClear) {
            notifyItemChanged(position);
        }

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

    public List<Uri> getSelectedItems() {
        return new ArrayList<>(seleccionadas);
    }

    public List<Uri> getRetainedItems() {
        List<Uri> retained = new ArrayList<>(lista);
        if (seleccionadas.size() > 0) {
            retained.removeAll(seleccionadas);
        }
        return retained;
    }

    public List<Uri> discardSelectedItems() {
        List<Uri> discardedUris = getSelectedItems();
        lista.removeAll(discardedUris);
        seleccionadas.clear();
        selectionMode = false;

        // Reiniciar el índice de enfoque
        selectedIndex = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
        listener.onSelectionChanged(0);
        return discardedUris;
    }
}
