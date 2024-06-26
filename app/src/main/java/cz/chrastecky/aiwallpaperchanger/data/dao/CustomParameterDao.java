package cz.chrastecky.aiwallpaperchanger.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.relation.CustomParameterWithValues;

@Dao
public interface CustomParameterDao {
    @Insert
    void create(CustomParameter customParameter);

    @Transaction
    @Query("select * from CustomParameter")
    List<CustomParameterWithValues> getAll();
}
