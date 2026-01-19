package com.example.finalshield.Adaptadores;

public interface AdaptadorListener {
    void onBorrarClick(int position);
    void onItemClick(int position);
    void onDescifrarClick(int position); // Servirá para descifrar o recifrar según el fragmento
}
