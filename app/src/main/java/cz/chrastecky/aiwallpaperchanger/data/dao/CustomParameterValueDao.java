package cz.chrastecky.aiwallpaperchanger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;

@Dao
public interface CustomParameterValueDao {
    @Insert
    void create(CustomParameterValue value);

    @Insert
    void createMultiple(List<CustomParameterValue> values);
}
