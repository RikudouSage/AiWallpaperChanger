package cz.chrastecky.aiwallpaperchanger.action;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public interface WallpaperAction {
    @NonNull
    String getId();

    boolean setWallpaper(Context context, Bitmap wallpaper);
}
