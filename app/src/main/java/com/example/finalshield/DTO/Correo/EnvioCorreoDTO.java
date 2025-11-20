package com.example.finalshield.DTO.Correo;

import com.example.finalshield.DTO.Usuario.UsuarioDTO;

public class EnvioCorreoDTO {
    private Integer idEnvioCorreo;
    private String fechaEnvio;
    private UsuarioDTO usuarioEmisorDTO;
    private CorreoDTO correoDTO;
    public Integer getIdEnvioCorreo() {
        return idEnvioCorreo;
    }
    public void setIdEnvioCorreo(Integer idEnvioCorreo) {
        this.idEnvioCorreo = idEnvioCorreo;
    }
    public String getFechaEnvio() {
        return fechaEnvio;
    }
    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
    public UsuarioDTO getUsuarioEmisorDTO() {
        return usuarioEmisorDTO;
    }
    public void setUsuarioEmisorDTO(UsuarioDTO usuarioEmisorDTO) {
        this.usuarioEmisorDTO = usuarioEmisorDTO;
    }
    public CorreoDTO getCorreoDTO() {
        return correoDTO;
    }
    public void setCorreoDTO(CorreoDTO correoDTO) {
        this.correoDTO = correoDTO;
    }
}
