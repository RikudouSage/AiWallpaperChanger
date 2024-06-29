package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import cz.chrastecky.aiwallpaperchanger.data.AppDatabase;

public class DatabaseHelper {
    @NonNull
    public static AppDatabase getDatabase(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "ai_wallpaper_changer").build();
    }
}
