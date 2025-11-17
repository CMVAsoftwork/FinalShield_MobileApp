package com.example.finalshield.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat FORMATO_ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    private static final SimpleDateFormat FORMATO_MOSTRAR = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public static String formatearFechaParaMostrar(String fechaISO) {
        if (fechaISO == null || fechaISO.isEmpty()) {
            return "N/A";
        }
        try {
            Date fecha = FORMATO_ISO.parse(fechaISO.substring(0, 19));

            return FORMATO_MOSTRAR.format(fecha);
        } catch (ParseException e) {
            e.printStackTrace();
            return fechaISO;
        }
    }
}
