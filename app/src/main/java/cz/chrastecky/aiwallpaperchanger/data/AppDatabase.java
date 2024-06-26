package cz.chrastecky.aiwallpaperchanger.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import cz.chrastecky.aiwallpaperchanger.data.dao.CustomParameterDao;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;

@Database(entities = {CustomParameter.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    abstract public CustomParameterDao customParameters();
}
