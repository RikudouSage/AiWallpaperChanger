package cz.chrastecky.aiwallpaperchanger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;

@Dao
public interface CustomParameterDao {
    @Insert
    void create(CustomParameter customParameter);
    @Query("select * from custom_parameters")
    List<CustomParameter> getAll();
}
