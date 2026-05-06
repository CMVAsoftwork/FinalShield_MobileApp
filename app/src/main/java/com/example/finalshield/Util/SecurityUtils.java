package com.example.finalshield.Util;

import android.util.Log;
import java.io.File;
import java.io.RandomAccessFile;
import java.security.SecureRandom;

public class SecurityUtils {

    /**
     * Tritura un archivo antes de borrarlo físicamente.
     * Sobrescribe el contenido con bytes aleatorios de grado criptográfico.
     */
    public static void borrarPermanente(File file) {
        if (file == null || !file.exists()) return;

        try (RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            long length = raf.length();
            SecureRandom random = new SecureRandom();
            byte[] buffer = new byte[8192]; // Buffer de 8KB para velocidad
            long totalEscrito = 0;

            while (totalEscrito < length) {
                random.nextBytes(buffer);
                int aEscribir = (int) Math.min(buffer.length, length - totalEscrito);
                raf.write(buffer, 0, aEscribir);
                totalEscrito += aEscribir;
            }

            // Obligamos al hardware a confirmar la escritura de la "basura"
            raf.getFD().sync();
            raf.close();

            // Ahora sí, eliminamos el puntero del sistema
            if (file.delete()) {
                Log.d("FINALSHIELD_SECURITY", "Archivo triturado y eliminado: " + file.getName());
            }
        } catch (Exception e) {
            Log.e("FINALSHIELD_ERROR", "Fallo en triturado, aplicando borrado estándar: " + e.getMessage());
            file.delete(); // Borrado normal como último recurso
        }
    }
}