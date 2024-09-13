package cz.chrastecky.aiwallpaperchanger.easymode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class EasyModeImageManager {
    private final Context context;
    private final Logger logger;

    public EasyModeImageManager(final Context context) {
        this.context = context;
        this.logger = new Logger(context);
    }

    public CompletableFuture<Bitmap> getImage(@NonNull final String path) {
        final CompletableFuture<Bitmap> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final File existingFile = new File(context.getCacheDir() + "/easy-mode", path);
            if (existingFile.exists()) {
                future.complete(BitmapFactory.decodeFile(existingFile.getAbsolutePath()));
                return;
            }

            final OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                    .url(BuildConfig.EXAMPLES_URL + "/_easy_mode" + path + ".png")
                    .build();

            try (final Response response = client.newCall(request).execute()) {
                if (response.body() == null) {
                    future.completeExceptionally(new RuntimeException("Failed getting image body"));
                    return;
                }
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new RuntimeException("Failed getting image: " + response.code()));
                    return;
                }
                Bitmap result = BitmapFactory.decodeStream(response.body().byteStream());

                if (!existingFile.getParentFile().exists()) {
                    existingFile.getParentFile().mkdirs();
                }
                existingFile.createNewFile();

                FileOutputStream outputStream = new FileOutputStream(existingFile, false);
                result.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();

                future.complete(result);
            } catch (IOException e) {
                logger.error("FullWeather", "Got an Exception when getting weather", e);
                future.completeExceptionally(e);
            }
        }, context);

        return future;
    }
}
