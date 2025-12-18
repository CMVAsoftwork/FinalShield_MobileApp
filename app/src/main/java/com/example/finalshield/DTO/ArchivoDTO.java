package com.example.finalshield.DTO;

public class ArchivoDTO {
    private Integer id;
    private String nombreArchivo;
    private String estado;
    private String tipoArchivo;
    private Long tamano;
    private String rutaArchivo;
    private String fechaSubida;
    private Integer idUsuario;
    private Integer idCarpetaMonitorizada;

    public ArchivoDTO() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getEstado() {
        return estado;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public Long getTamano() {
        return tamano;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public Integer getIdCarpetaMonitorizada() {
        return idCarpetaMonitorizada;
    }
}
