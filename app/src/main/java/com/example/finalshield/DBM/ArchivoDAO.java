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

    @Query("SELECT * FROM archivo_metadata WHERE estaCifrado = 0")
    List<ArchivoMetadata> getAllDescifrados();
}