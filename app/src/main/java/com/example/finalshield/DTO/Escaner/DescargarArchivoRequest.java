package com.example.finalshield.DTO.Escaner;

public class DescargarArchivoRequest {
    private Integer idArchivo;
    private String clavePersonal;

    public DescargarArchivoRequest(Integer idArchivo, String clavePersonal) {
        this.idArchivo = idArchivo;
        this.clavePersonal = clavePersonal;
    }
}
