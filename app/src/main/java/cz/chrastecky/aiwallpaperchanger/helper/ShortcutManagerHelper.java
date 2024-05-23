package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Collections;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.activity.TriggerNextImageActivity;

public class ShortcutManagerHelper {
    public static final String NEXT_IMAGE_SHORTCUT = "next";

    public static void createShortcuts(Context context) {

        Intent intent = new Intent(TriggerNextImageActivity.ACTION_NAME)
                .setPackage(BuildConfig.APPLICATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, NEXT_IMAGE_SHORTCUT)
                .setShortLabel(context.getString(R.string.app_shortcut_next_image))
                .setLongLabel(context.getString(R.string.app_shortcut_next_image))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher_foreground))
                .setIntent(intent)
                .build();

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut);
    }

    public static void hideShortcuts(Context context) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(NEXT_IMAGE_SHORTCUT));
    }

    public static boolean hasShortcut(Context context, String name) {
        return ShortcutManagerCompat.getDynamicShortcuts(context)
                .stream()
                .anyMatch(shortcut -> shortcut.getId().equals(name));
    }
}
