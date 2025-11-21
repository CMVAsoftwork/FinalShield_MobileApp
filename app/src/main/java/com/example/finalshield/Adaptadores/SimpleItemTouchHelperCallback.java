package com.example.finalshield.Adaptadores;

import static androidx.recyclerview.widget.ItemTouchHelper.Callback.makeMovementFlags;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ImageAdapter mAdapter;

    public SimpleItemTouchHelperCallback(ImageAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Permitimos el arrastre en todas las direcciones (GridLayoutManager)
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        // Usamos el método existente en ImageAdapter
        mAdapter.moverItemEficiente(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // No implementado
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // Permitimos que el arrastre inicie con una pulsación larga
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    // Opcional: Para feedback visual durante el arrastre (cambio de fondo, etc.)
    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // 1: DRAG_OVER (arrastrando)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // Puedes cambiar la apariencia del item arrastrado aquí, ej:
            // viewHolder.itemView.setBackgroundColor(Color.LTGRAY);
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Cuando se suelta el item, se limpia el fondo
        // viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
    }
}