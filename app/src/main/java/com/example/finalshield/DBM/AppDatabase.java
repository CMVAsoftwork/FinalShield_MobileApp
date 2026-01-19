package com.example.finalshield.DBM;
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.finalshield.DBM.ArchivoDAO;
import com.example.finalshield.DBM.Converters;
import com.example.finalshield.Model.ArchivoMetadata;

@Database(entities = {ArchivoMetadata.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ArchivoDAO archivoDAO();

    // Méodo para obtener la instancia única
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "shield_vault_db")
                            .fallbackToDestructiveMigration() // Útil durante desarrollo
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}