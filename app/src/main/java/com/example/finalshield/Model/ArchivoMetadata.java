package com.example.finalshield.Model;
import android.net.Uri;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.finalshield.DBM.Converters;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;

@Entity(tableName = "archivos_cifrados_metadata")
@TypeConverters({Converters.class})
public class ArchivoMetadata implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long idLocal;

    private Integer idArchivoServidor;
    private String nombre;
    private Uri uriOriginal;
    private long tamanioBytes;
    private Date fechaSeleccion;
    private boolean estaCifrado;
    private String tipoArchivo;
    private String rutaServidor;
    private String rutaLocalEncriptada;
    private String rutaLocalDescifrado;

    // Constructor vacío OBLIGATORIO para Room
    public ArchivoMetadata() {}

    // Constructor para selección inicial (ignorado por Room)
    @Ignore
    public ArchivoMetadata(String nombre, Uri uri, long tamanioBytes, Date fechaSeleccion, boolean estaCifrado) {
        this.nombre = nombre;
        this.uriOriginal = uri;
        this.tamanioBytes = tamanioBytes;
        this.fechaSeleccion = fechaSeleccion;
        this.estaCifrado = estaCifrado;
    }

    // Constructor desde backend (ignorado por Room)
    @Ignore
    public ArchivoMetadata(Archivo archivo) {
        this.idArchivoServidor = archivo.getIdArchivo();
        this.nombre = archivo.getNombreArchivo();
        this.tamanioBytes = archivo.getTamano();
        this.estaCifrado = "Cifrado".equals(archivo.getEstado());
        this.tipoArchivo = archivo.getTipoArchivo();
        this.rutaServidor = archivo.getRutaArchivo();
    }

    // Getters y Setters (todos)
    public long getIdLocal() { return idLocal; }
    public void setIdLocal(long idLocal) { this.idLocal = idLocal; }

    public Integer getIdArchivoServidor() { return idArchivoServidor; }
    public void setIdArchivoServidor(Integer idArchivoServidor) { this.idArchivoServidor = idArchivoServidor; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Uri getUriOriginal() { return uriOriginal; }
    public void setUriOriginal(Uri uriOriginal) { this.uriOriginal = uriOriginal; }

    public long getTamanioBytes() { return tamanioBytes; }
    public void setTamanioBytes(long tamanioBytes) { this.tamanioBytes = tamanioBytes; }

    public Date getFechaSeleccion() { return fechaSeleccion; }
    public void setFechaSeleccion(Date fechaSeleccion) { this.fechaSeleccion = fechaSeleccion; }

    public boolean isEstaCifrado() { return estaCifrado; }
    public void setEstaCifrado(boolean estaCifrado) { this.estaCifrado = estaCifrado; }

    public String getTipoArchivo() { return tipoArchivo; }
    public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }

    public String getRutaServidor() { return rutaServidor; }
    public void setRutaServidor(String rutaServidor) { this.rutaServidor = rutaServidor; }

    public String getRutaLocalEncriptada() { return rutaLocalEncriptada; }
    public void setRutaLocalEncriptada(String rutaLocalEncriptada) { this.rutaLocalEncriptada = rutaLocalEncriptada; }

    public String getRutaLocalDescifrado() { return rutaLocalDescifrado; }
    public void setRutaLocalDescifrado(String rutaLocalDescifrado) { this.rutaLocalDescifrado = rutaLocalDescifrado; }

    public String getTamanioFormateado() {
        long size = tamanioBytes;
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public String getEstadoTexto() {
        return estaCifrado ? "Cifrado" : "Descifrado";
    }
}