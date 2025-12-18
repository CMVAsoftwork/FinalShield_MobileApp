package com.example.finalshield.DTO.Escaner;

public class ArchivoEscaneadoDTO {
    private Integer idArchivoEcaneado;
    private String nombreArchivoEscaneado;
    private String fechaSubida;
    private String hashSha256;

    public Integer getIdArchivoEcaneado() {
        return idArchivoEcaneado;
    }

    public String getNombreArchivoEscaneado() {
        return nombreArchivoEscaneado;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public String getHashSha256() {
        return hashSha256;
    }
}
