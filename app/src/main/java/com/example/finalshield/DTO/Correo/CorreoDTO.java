package com.example.finalshield.DTO.Correo;

import java.util.List;

public class CorreoDTO {
    private Integer idCorreo;
    private String contenidoCifrado;
    private String claveCifDes;
    private String estatus;
    private List<ArchivoCorreoDTO> archivosAdjuntos;

    public Integer getIdCorreo() {
        return idCorreo;
    }

    public String getContenidoCifrado() {
        return contenidoCifrado;
    }

    public String getClaveCifDes() {
        return claveCifDes;
    }

    public List<ArchivoCorreoDTO> getArchivosAdjuntos() {
        return archivosAdjuntos;
    }

    public void setIdCorreo(Integer idCorreo) {
        this.idCorreo = idCorreo;
    }

    public void setContenidoCifrado(String contenidoCifrado) {
        this.contenidoCifrado = contenidoCifrado;
    }

    public void setClaveCifDes(String claveCifDes) {
        this.claveCifDes = claveCifDes;
    }

    public void setEstatus(String estatus) {
        this.estatus = estatus;
    }

    public void setArchivosAdjuntos(List<ArchivoCorreoDTO> archivosAdjuntos) {
        this.archivosAdjuntos = archivosAdjuntos;
    }
}
