package com.example.finalshield.DTO;

public class DescifradoRequest {
    private String textoCifradoBase64;
    private String claveBase64;

    public DescifradoRequest() {}

    public DescifradoRequest(String textoCifradoBase64, String claveBase64) {
        this.textoCifradoBase64 = textoCifradoBase64;
        this.claveBase64 = claveBase64;
    }

    public String getTextoCifradoBase64() {
        return textoCifradoBase64;
    }

    public void setTextoCifradoBase64(String textoCifradoBase64) {
        this.textoCifradoBase64 = textoCifradoBase64;
    }

    public String getClaveBase64() {
        return claveBase64;
    }

    public void setClaveBase64(String claveBase64) {
        this.claveBase64 = claveBase64;
    }
}
