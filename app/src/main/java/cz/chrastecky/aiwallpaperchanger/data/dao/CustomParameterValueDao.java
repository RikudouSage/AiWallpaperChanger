package cz.chrastecky.aiwallpaperchanger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;

@Dao
public interface CustomParameterValueDao {
    @Insert
    void create(CustomParameterValue value);
}
