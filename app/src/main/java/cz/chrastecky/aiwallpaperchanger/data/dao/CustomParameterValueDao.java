package cz.chrastecky.aiwallpaperchanger.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Upsert;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;

@Dao
public interface CustomParameterValueDao {
    @Insert
    void create(CustomParameterValue value);

    @Upsert
    void upsertMultiple(List<CustomParameterValue> values);

    @Delete
    void delete(CustomParameterValue value);
}
