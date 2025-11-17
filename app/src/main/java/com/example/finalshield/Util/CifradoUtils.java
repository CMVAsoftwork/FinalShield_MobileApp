package com.example.finalshield.Util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CifradoUtils {
    private static final String ALGORITMO = "AES";
    private static final String ALGORITMO_COMPLETO = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    public static SecretKey base64AClave(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return new SecretKeySpec(bytes, ALGORITMO);
    }
    public static String descifrarTexto(String textoCifradoBase64ConIV, String claveBase64) throws GeneralSecurityException {
        try {
            SecretKey clave = base64AClave(claveBase64);

            byte[] combinado = Base64.decode(textoCifradoBase64ConIV, Base64.DEFAULT);

            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combinado, 0, iv, 0, IV_SIZE);

            byte[] cifrado = new byte[combinado.length - IV_SIZE];
            System.arraycopy(combinado, IV_SIZE, cifrado, 0, cifrado.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO_COMPLETO);
            cipher.init(Cipher.DECRYPT_MODE, clave, ivSpec);

            byte[] descifrado = cipher.doFinal(cifrado);
            return new String(descifrado, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new GeneralSecurityException("Error al descifrar el texto. La clave o el formato son incorrectos.", e);
        }
    }
}
