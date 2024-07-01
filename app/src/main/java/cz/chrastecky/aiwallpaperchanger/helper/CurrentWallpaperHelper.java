package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CurrentWallpaperHelper {
    @Nullable
    public static File getFile(@NonNull Context context) {
        File imageFile = getFileRaw(context);
        if (!imageFile.exists()) {
            return null;
        }

        return imageFile;
    }

    @Nullable
    public static Bitmap getBitmap(@NonNull Context context) {
        final File file = getFile(context);
        if (file == null) {
            return null;
        }

        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    @NonNull
    public static File save(@NonNull Context context, @NonNull Bitmap bitmap) throws IOException {
        final File file = getFileRaw(context);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(file, false);
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);
        outputStream.close();

        return file;
    }

    @NonNull
    @Contract("_ -> new")
    private static File getFileRaw(@NonNull Context context) {
        return new File(context.getFilesDir(), "currentImage.webp");
    }
 }
