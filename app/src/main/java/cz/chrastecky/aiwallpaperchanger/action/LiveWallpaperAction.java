package cz.chrastecky.aiwallpaperchanger.action;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import cz.chrastecky.annotationprocessor.InjectedWallpaperAction;

@InjectedWallpaperAction
public class LiveWallpaperAction implements WallpaperAction {
    public static final String ID = "live";
    public static final String INTENT_ACTION = "cz.chrastecky.aiwallpaperchanger.UPDATE_LIVE_WALLPAPER";

    @NonNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean setWallpaper(@NonNull Context context, @NonNull Bitmap wallpaper) {
        Intent intent = new Intent(INTENT_ACTION);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        return true;
    }
}
