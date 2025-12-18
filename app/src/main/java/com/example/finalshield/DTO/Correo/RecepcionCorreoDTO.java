package com.example.finalshield.DTO.Correo;

import com.example.finalshield.DTO.Usuario.UsuarioDTO;

public class RecepcionCorreoDTO {
    private Integer idRecepcionCorreo;
    private String fechaRecepcion;
    private UsuarioDTO usuarioRecepcionDTO;
    private EnvioCorreoDTO envioRecepcionDTO;
    public Integer getIdRecepcionCorreo() {
        return idRecepcionCorreo;
    }

    public void setIdRecepcionCorreo(Integer idRecepcionCorreo) {
        this.idRecepcionCorreo = idRecepcionCorreo;
    }
    public String getFechaRecepcion() {
        return fechaRecepcion;
    }
    public void setFechaRecepcion(String fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }
    public UsuarioDTO getUsuarioRecepcionDTO() {
        return usuarioRecepcionDTO;
    }
    public void setUsuarioRecepcionDTO(UsuarioDTO usuarioRecepcionDTO) {
        this.usuarioRecepcionDTO = usuarioRecepcionDTO;
    }
    public EnvioCorreoDTO getEnvioRecepcionDTO() {
        return envioRecepcionDTO;
    }
    public void setEnvioRecepcionDTO(EnvioCorreoDTO envioRecepcionDTO) {
        this.envioRecepcionDTO = envioRecepcionDTO;
    }
}
