package cz.chrastecky.aiwallpaperchanger.action;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.io.IOException;

import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.annotationprocessor.InjectedWallpaperAction;

@InjectedWallpaperAction
public class StaticWallpaperAction implements WallpaperAction {
    public static final String ID = "static";

    @NonNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean setWallpaper(final Context context, final Bitmap wallpaper) {
        final Logger logger = new Logger(context);
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        try {
            int status = wallpaperManager.setBitmap(wallpaper, null, true);
            logger.debug("StaticWallpaper", "The wallpaper has been set");
            logger.debug("StaticWallpaper", "Set wallpaper status: " + status);

            return status != 0;
        } catch (IOException e) {
            logger.error("StaticWallpaper", "Caught IOException", e);
            return false;
        }
    }
}
