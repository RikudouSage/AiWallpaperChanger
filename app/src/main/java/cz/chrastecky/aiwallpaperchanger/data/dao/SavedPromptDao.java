package cz.chrastecky.aiwallpaperchanger.data.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.SavedPrompt;

@Dao
public interface SavedPromptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void createOrUpdate(SavedPrompt entity);

    @Delete
    void delete(SavedPrompt entity);

    @Transaction
    @Query("select * from SavedPrompt")
    List<SavedPrompt> getAll();

    @Nullable
    @Transaction
    @Query("select * from SavedPrompt where name = :name")
    SavedPrompt findByName(String name);
}
