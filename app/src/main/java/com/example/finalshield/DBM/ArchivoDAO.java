package com.example.finalshield.DBM;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.finalshield.Model.ArchivoMetadata;

import java.util.List;

@Dao
public interface ArchivoDAO {
    @Insert
    void insert(ArchivoMetadata archivo);

    @Update
    void update(ArchivoMetadata archivo);

    @Delete
    void delete(ArchivoMetadata archivo);

    @Query("SELECT * FROM archivos_cifrados_metadata")
    List<ArchivoMetadata> getAll();

    // Filtro para la lista de DESCIFRADOS (Esta no cambia)
    @Query("SELECT * FROM archivos_cifrados_metadata WHERE estaCifrado = 0")
    List<ArchivoMetadata> getAllDescifrados();

    // --- NUEVAS CONSULTAS PARA EVITAR ERRORES ---

    // Mantenemos esta por si alguna otra parte de tu código la usa (Compatibilidad)
    @Query("SELECT * FROM archivos_cifrados_metadata WHERE estaCifrado = 1")
    List<ArchivoMetadata> getAllCifrados();
    @Query("SELECT * FROM archivos_cifrados_metadata WHERE estaCifrado = 1 AND origen = 'ARCHIVOS'")
    List<ArchivoMetadata> getAllCifradosPrincipales();

    @Query("SELECT * FROM archivos_cifrados_metadata WHERE estaCifrado = 1 AND origen = 'ESCANEO'")
    List<ArchivoMetadata> getAllCifradosEscaneo();
}