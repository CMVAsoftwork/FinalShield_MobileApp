package com.example.finalshield.Model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Archivo implements Serializable {
    @SerializedName("idArchivo")
    private Integer idArchivo;

    @SerializedName("nombreArchivo")
    private String nombreArchivo;

    @SerializedName("estado")
    private String estado;

    @SerializedName("tipoArchivo")
    private String tipoArchivo;

    @SerializedName("rutaArchivo")
    private String rutaArchivo;

    @SerializedName("tamano")
    private long tamano;

    @SerializedName("fechaSubida")
    private String fechaSubida;

    public Integer getIdArchivo() {
        return idArchivo;
    }

    public void setIdArchivo(Integer idArchivo) {
        this.idArchivo = idArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public long getTamano() {
        return tamano;
    }

    public void setTamano(long tamano) {
        this.tamano = tamano;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}