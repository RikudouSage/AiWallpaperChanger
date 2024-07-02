package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;

public class ThreadHelper {
    public static void setupErrorHandler(@NonNull final Logger logger) {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> logger.error("UncaughtException", "Got uncaught exception", error)
        );
    }

    @NonNull
    public static Thread runInThread(@NonNull final Runnable runnable, @NonNull final Context context) {
        Thread thread = new Thread(() -> {
            final Logger logger = new Logger(context);
            setupErrorHandler(logger);

            runnable.run();
        });
        thread.start();

        return thread;
    }
}
