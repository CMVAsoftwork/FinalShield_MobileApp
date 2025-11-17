package com.example.finalshield.DTO;

public class DescifradoRequest {
    private String claveBase64;

    public DescifradoRequest() {}

    public DescifradoRequest(String claveBase64) {
        this.claveBase64 = claveBase64;
    }

    public String getClaveBase64() {
        return claveBase64;
    }

    public void setClaveBase64(String claveBase64) {
        this.claveBase64 = claveBase64;
    }
}
