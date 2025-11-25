package com.example.finalshield.Adaptadores;

import android.net.Uri;

import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

public class SharedImageViewModel extends ViewModel {

    // Almacena la lista de URIs.
    private List<Uri> imageUriList;

    public void setImageUriList(List<Uri> list) {
        this.imageUriList = list;
    }

    public List<Uri> getImageUriList() {
        // Devuelve la lista, o una lista vacía si es null.
        if (imageUriList == null) {
            return Collections.emptyList();
        }
        return imageUriList;
    }

    public void clearList() {
        // Limpia la lista después de que VistaImagenActivity la usa para liberar memoria.
        this.imageUriList = null;
    }
}
