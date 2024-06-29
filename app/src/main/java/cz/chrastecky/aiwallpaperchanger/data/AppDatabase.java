package cz.chrastecky.aiwallpaperchanger.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import cz.chrastecky.aiwallpaperchanger.data.dao.CustomParameterDao;
import cz.chrastecky.aiwallpaperchanger.data.dao.CustomParameterValueDao;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;

@Database(entities = {CustomParameter.class, CustomParameterValue.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    abstract public CustomParameterDao customParameters();
    abstract public CustomParameterValueDao customParameterValues();
}
