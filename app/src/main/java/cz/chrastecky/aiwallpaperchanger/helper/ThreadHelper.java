package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import cz.chrastecky.aiwallpaperchanger.activity.UncaughtErrorActivity;

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

    public static void setupGraphicalErrorHandler(@NonNull final Logger logger, @NonNull final Context context) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            logger.error("UncaughtException", "There was an uncaught exception", exception);

            Intent intent = new Intent(context, UncaughtErrorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }
}
