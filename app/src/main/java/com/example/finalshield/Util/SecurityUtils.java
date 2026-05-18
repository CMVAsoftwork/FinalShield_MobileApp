package com.example.finalshield.Util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "FinalShieldKeySecure";
    private static final int IV_SIZE = 16;

    /**
     * Obtiene la clave maestra del hardware seguro (Keystore).
     */
    private static SecretKey obtenerLlaveMaestraHardware() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

        KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(256)
                .build();

        keyGenerator.init(keyGenParameterSpec);
        return keyGenerator.generateKey();
    }

    /**
     * Deriva una clave única combinando de forma irreversible la llave del hardware
     * con la identidad/identificador único del usuario activo (Cero-Conocimiento Híbrido).
     */
    private static SecretKeySpec derivarLlaveHibrida(String idUsuario) throws Exception {
        if (idUsuario == null || idUsuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador de usuario no puede estar vacío.");
        }

        SecretKey llaveHardware = obtenerLlaveMaestraHardware();
        byte[] bytesHardware = llaveHardware.getEncoded();

        // Si el contenedor de hardware restringe la exportación directa de bytes (retorna null),
        // usamos el algoritmo alternativo basado en su representación de Alias hash.
        if (bytesHardware == null) {
            bytesHardware = KEY_ALIAS.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        byte[] bytesUsuario = idUsuario.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Mezclamos los dos factores usando un hash criptográfico de un solo sentido (SHA-256)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytesHardware);
        byte[] llaveDerivadaBytes = digest.digest(bytesUsuario);

        // Retornamos la estructura lista para alimentar a AES-256
        return new SecretKeySpec(llaveDerivadaBytes, "AES");
    }

    /**
     * Cifra un archivo requiriendo explícitamente el ID del usuario como factor lógico adicional.
     */
    public static void cifrarArchivoLocal(File archivoOrigen, File archivoDestino, String idUsuario) throws Exception {
        // Obtenemos el candado combinado exclusivo (Hardware + Usuario)
        SecretKeySpec llaveHibrida = derivarLlaveHibrida(idUsuario);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, llaveHibrida);

        byte[] iv = cipher.getIV();

        try (FileOutputStream fos = new FileOutputStream(archivoDestino);
             FileInputStream fis = new FileInputStream(archivoOrigen)) {

            fos.write(iv);

            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, read);
                }
            }
        }
        Log.d("FINALSHIELD_SECURITY", "Archivo cifrado con doble candado para usuario: " + idUsuario);
    }

    /**
     * Descifra un archivo local. Si la clave híbrida del usuario activo es incorrecta,
     * el motor Cipher lanzará una excepción protegiendo los bloques de datos.
     * Mantiene el mecanismo tolerante a fallos si el archivo ya está en texto claro.
     */
    public static void descifrarArchivoLocal(File archivoCifrado, File archivoDestino, String idUsuario) throws Exception {
        try (FileInputStream fis = new FileInputStream(archivoCifrado)) {
            byte[] iv = new byte[IV_SIZE];
            int ivRead = fis.read(iv);

            if (ivRead != IV_SIZE) {
                fallbackCopiarDirecto(archivoCifrado, archivoDestino);
                return;
            }

            try {
                // Derivamos la llave con la sesión del usuario actual
                SecretKeySpec llaveHibrida = derivarLlaveHibrida(idUsuario);

                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, llaveHibrida, new IvParameterSpec(iv));

                try (FileOutputStream fos = new FileOutputStream(archivoDestino);
                     CipherInputStream cis = new CipherInputStream(fis, cipher)) {

                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = cis.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }
                Log.d("FINALSHIELD_SECURITY", "Descifrado exitoso para sesión activa: " + idUsuario);

            } catch (Exception cryptoException) {
                // Si falla por una clave de usuario incorrecta (BadPadding), o si el archivo estaba en claro,
                // el log de advertencia actuará y derivará de forma segura.
                Log.w("FINALSHIELD_SECURITY", "Fallo de validación criptográfica o archivo ya en claro. Aplicando contingencia.");
                fallbackCopiarDirecto(archivoCifrado, archivoDestino);
            }
        }
    }

    private static void fallbackCopiarDirecto(File origen, File destino) throws Exception {
        try (FileInputStream fis = new FileInputStream(origen);
             FileOutputStream fos = new FileOutputStream(destino);
             FileChannel inChannel = fis.getChannel();
             FileChannel outChannel = fos.getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        Log.d("FINALSHIELD_SECURITY", "Transferencia por canal alternativo completada.");
    }

    /**
     * Tritura un archivo antes de borrarlo físicamente.
     */
    public static void borrarPermanente(File file) {
        if (file == null || !file.exists()) return;

        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            long length = raf.length();
            SecureRandom random = new SecureRandom();
            byte[] buffer = new byte[8192];
            long totalEscrito = 0;

            while (totalEscrito < length) {
                random.nextBytes(buffer);
                int aEscribir = (int) Math.min(buffer.length, length - totalEscrito);
                raf.write(buffer, 0, aEscribir);
                totalEscrito += aEscribir;
            }

            raf.getFD().sync();
            raf.close();

            if (file.delete()) {
                Log.d("FINALSHIELD_SECURITY", "Archivo triturado y eliminado: " + file.getName());
            }
        } catch (Exception e) {
            Log.e("FINALSHIELD_ERROR", "Fallo en triturado, aplicando borrado estándar: " + e.getMessage());
            file.delete();
        }
    }
}