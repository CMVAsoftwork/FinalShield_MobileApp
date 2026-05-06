package com.example.finalshield.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CargaViewModel extends ViewModel {
    private final MutableLiveData<Boolean> _finalizarCarga = new MutableLiveData<>(false);
    public LiveData<Boolean> finalizarCarga = _finalizarCarga;
    private int destinoDinamico = -1;

    public void setDestinoDinamico(int id) {
        this.destinoDinamico = id;
    }

    public int getDestinoDinamico() {
        return destinoDinamico;
    }

    public void terminarProceso() {
        // postValue es seguro para usar desde hilos de Retrofit o el MainThread
        _finalizarCarga.postValue(true);
    }

    public void resetear() {
        _finalizarCarga.postValue(false);
    }
}