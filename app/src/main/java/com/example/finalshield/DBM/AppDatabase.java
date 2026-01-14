package com.example.finalshield.DBM;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.DBM.Converters;
import com.example.finalshield.Model.ArchivoMetadata;

@Database(entities = {ArchivoMetadata.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ArchivoDAO archivoDAO();
}