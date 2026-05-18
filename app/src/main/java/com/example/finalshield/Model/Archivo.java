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
    private Long tamano; // Cambiado a Long por si el servidor llega a mandar null

    @SerializedName("fechaSubida")
    private String fechaSubida;

    // --- NUEVO OBJETO ANIDADO SERIALIZABLE ---
    @SerializedName("usuario")
    private UsuarioEmbed usuario;

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

    public Long getTamano() {
        return tamano;
    }

    public void setTamano(Long tamano) {
        this.tamano = tamano;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public UsuarioEmbed getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEmbed usuario) {
        this.usuario = usuario;
    }

    // =========================================================================
    // Subclase estática interna para interceptar el JSON de usuario sin romper nada
    // =========================================================================
    public static class UsuarioEmbed implements Serializable {
        @SerializedName("idUsuario")
        private Integer idUsuario;

        @SerializedName("nombre")
        private String nombre;

        @SerializedName("correo")
        private String correo;

        public Integer getIdUsuario() {
            return idUsuario;
        }

        public void setIdUsuario(Integer idUsuario) {
            this.idUsuario = idUsuario;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getCorreo() {
            return correo;
        }

        public void setCorreo(String correo) {
            this.correo = correo;
        }
    }
}