package cz.chrastecky.aiwallpaperchanger.helper;

import androidx.annotation.NonNull;

public class ThreadHelper {
    public static void setupErrorHandler(@NonNull final Logger logger) {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> logger.error("UncaughtException", "Got uncaught exception", error)
        );
    }
}
