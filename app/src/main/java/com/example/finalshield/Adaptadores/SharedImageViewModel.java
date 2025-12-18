package com.example.finalshield.Adaptadores;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SharedImageViewModel extends ViewModel {

    // LISTA PRINCIPAL (COMBINADA - CÁMARA + GALERÍA)
    private final MutableLiveData<List<Uri>> imageUriList = new MutableLiveData<>(new ArrayList<>());

    // LISTA EXCLUSIVA: SOLO CÁMARA (Para reordenamiento y gestión separada)
    private final MutableLiveData<List<Uri>> cameraOnlyUriList = new MutableLiveData<>(new ArrayList<>());


    // --- MÉTODOS PARA LA LISTA PRINCIPAL (COMBINADA) ---

    public void setImageUriList(List<Uri> list) {
        if (list == null) {
            this.imageUriList.setValue(new ArrayList<>());
        } else {
            this.imageUriList.setValue(list);
        }
    }

    public List<Uri> getImageUriList() {
        List<Uri> currentList = imageUriList.getValue();
        return currentList != null ? currentList : Collections.emptyList();
    }

    public LiveData<List<Uri>> getLiveImageUriList() {
        return imageUriList;
    }

    public void clearList() {
        this.imageUriList.setValue(new ArrayList<>());
    }

    // --- MÉTODOS PARA LA NUEVA LISTA (SOLO CÁMARA) ---

    public void setCameraOnlyUriList(List<Uri> list) {
        if (list == null) {
            this.cameraOnlyUriList.setValue(new ArrayList<>());
        } else {
            this.cameraOnlyUriList.setValue(list);
        }
    }

    public List<Uri> getCameraOnlyUriList() {
        List<Uri> currentList = cameraOnlyUriList.getValue();
        return currentList != null ? currentList : Collections.emptyList();
    }

    public LiveData<List<Uri>> getLiveCameraOnlyUriList() {
        return cameraOnlyUriList;
    }

    public void clearCameraOnlyList() {
        this.cameraOnlyUriList.setValue(new ArrayList<>());
    }
}
