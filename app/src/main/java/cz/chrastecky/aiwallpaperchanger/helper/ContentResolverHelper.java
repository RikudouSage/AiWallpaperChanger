package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriPermission;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.io.FileOutputStream;
import java.io.IOException;

public class ContentResolverHelper {
    public static boolean canAccessDirectory(@NonNull Context context, @NonNull Uri directory) {
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.getUri().equals(directory)) {
                return true;
            }
        }

        return false;
    }

    public static void storeBitmap(@NonNull Context context, @NonNull Uri directory, @NonNull String filename, @NonNull Bitmap content) {
        if (!canAccessDirectory(context, directory)) {
            Log.e("StoreBitmapError", "Access to the directory " + directory + " is not granted");
            return;
        }
        DocumentFile directoryDocument = DocumentFile.fromTreeUri(context, directory);
        if (directoryDocument == null || !directoryDocument.exists()) {
            Log.e("StoreBitmapError", "Cannot store the image in " + directory + ", the directory either does not exist or there's no permission to view it.");
            return;
        }

        DocumentFile file = directoryDocument.createFile("image/png", filename);
        ContentResolver contentResolver = context.getContentResolver();
        assert file != null;
        try (
                ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(file.getUri(), "w");
                FileOutputStream outputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
        ) {
            content.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            Log.e("StoreBitmapError", "Could not open file " + file + " for writing.");
        }
    }
}
