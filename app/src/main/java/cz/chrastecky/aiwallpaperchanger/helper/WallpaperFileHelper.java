package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.sharing.AppFileProvider;

public class WallpaperFileHelper {
    private static final String FILENAME = "currentImage.webp";

    @Nullable
    public static File getFile(@NonNull Context context, @NonNull String filename) {
        File imageFile = getFileRaw(context, filename);
        if (!imageFile.exists()) {
            return null;
        }

        return imageFile;
    }

    public static File getFile(@NonNull Context context) {
        return getFile(context, FILENAME);
    }

    @Nullable
    public static Bitmap getBitmap(@NonNull Context context, @NonNull String filename) {
        final File file = getFile(context, filename);
        if (file == null) {
            return null;
        }

        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public static Bitmap getBitmap(@NonNull Context context) {
        return getBitmap(context, FILENAME);
    }

    @NonNull
    public static File save(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String filename) throws IOException {
        final Logger logger = new Logger(context);
        logger.debug("WallpaperFileHelper", "Trying to save a bitmap to " + filename);

        final File file = getFileRaw(context, filename);
        if (file.exists()) {
            logger.debug("WallpaperFileHelper", "File already exists, deleting");
            file.delete();
        }
        file.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(file, false);

        logger.debug("WallpaperFileHelper", "Trying to compress the bitmap into webp");
        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);
        logger.debug("WallpaperFileHelper", "Bitmap compressed");
        outputStream.close();

        return file;
    }

    @NonNull
    public static File save(@NonNull Context context, @NonNull Bitmap bitmap) throws IOException {
        return save(context, bitmap, FILENAME);
    }

    @Nullable
    public static Uri getShareableUri(@NonNull Context context, @NonNull String filename) {
        final File file = getFile(context, filename);
        if (file == null) {
            return null;
        }
        return AppFileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".files_provider", file);
    }

    public static Uri getShareableUri(@NonNull Context context) {
        return getShareableUri(context, FILENAME);
    }

    @Nullable
    public static Intent getShareIntent(@NonNull Context context, @NonNull String filename) {
        final Uri file = getShareableUri(context, filename);
        if (file == null) {
            return null;
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, file);
        intent.setType("image/webp");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return Intent.createChooser(intent, null);
    }

    @Nullable
    public static Intent getShareIntent(@NonNull Context context, @NonNull File file) {
        return getShareIntent(context, file.getName());
    }

    @Nullable
    public static Intent getShareIntent(@NonNull Context context) {
        return getShareIntent(context, FILENAME);
    }

    @NonNull
    @Contract("_, _ -> new")
    private static File getFileRaw(@NonNull Context context, @NonNull String filename) {
        return new File(context.getFilesDir(), filename);
    }
 }
