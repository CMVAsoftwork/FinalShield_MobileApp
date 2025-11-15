package com.example.finalshield.DTO;

public class ClaveRequest {
    private String clave;

    public ClaveRequest() {}
    public ClaveRequest(String clave) {
        this.clave = clave;
    }
    public String getClave() {
        return clave;
    }
    public void setClave(String clave) {
        this.clave = clave;
    }
}
