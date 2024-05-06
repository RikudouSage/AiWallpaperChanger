package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.NonNull;

public class ContentResolverHelper {
    public static boolean canAccessDirectory(@NonNull Context context, @NonNull Uri directory) {
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.getUri().equals(directory)) {
                return true;
            }
        }

        return false;
    }
}
