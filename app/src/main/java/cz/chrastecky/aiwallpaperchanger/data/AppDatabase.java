package cz.chrastecky.aiwallpaperchanger.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import cz.chrastecky.aiwallpaperchanger.data.converter.ListConverter;
import cz.chrastecky.aiwallpaperchanger.data.dao.CustomParameterDao;
import cz.chrastecky.aiwallpaperchanger.data.dao.CustomParameterValueDao;
import cz.chrastecky.aiwallpaperchanger.data.dao.SavedPromptDao;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;
import cz.chrastecky.aiwallpaperchanger.data.entity.SavedPrompt;

@Database(entities = {CustomParameter.class, CustomParameterValue.class, SavedPrompt.class}, version = 4)
@TypeConverters(ListConverter.class)
public abstract class AppDatabase extends RoomDatabase {
    abstract public CustomParameterDao customParameters();
    abstract public CustomParameterValueDao customParameterValues();
    abstract public SavedPromptDao savedPrompts();
}
