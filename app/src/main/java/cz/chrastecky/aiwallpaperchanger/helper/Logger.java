package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

public class Logger {

    private final Context context;

    public Logger(Context context) {
        this.context = context;
    }

    private enum Type {
        Debug,
        Error,
    }

    public void debug(String tag, String message) {
        Log.d(tag, message);
        log(Type.Debug, tag, message);
    }

    public void debug(String tag, String message, Throwable throwable) {
        Log.d(tag, message, throwable);
        log(Type.Debug, tag, message, throwable);
    }

    public void error(String tag, String message) {
        Log.e(tag, message);
        log(Type.Error, tag, message);
    }

    public void error(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        log(Type.Error, tag, message, throwable);
    }

    private void log(Type type, String tag, String message) {
        log(type, tag, message, null);
    }

    private void log(@NonNull Type type, String tag, String message, @Nullable Throwable throwable) {
        String date = DateFormat.format("yyyy-MM-dd", new Date()).toString();
        String dateFull = DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()).toString();
        String targetMessage = "[" + dateFull + "] [" + type + "]" + "[" + tag + "]: " + message;
        if (throwable != null) {
            targetMessage += "\nException: " + throwable.getMessage() + "\nStack trace:\n\t" + Arrays.stream(throwable.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n\t"));
        }
        targetMessage += "\n";

        try {
            File outFile = new File(context.getFilesDir(), "log." + date + ".txt");
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(outFile, true);
            outStream.write(targetMessage.getBytes(StandardCharsets.UTF_8));
            outStream.close();
        } catch (IOException e) {
            Log.e("AiWallpaperChanger", "Failed exporting log, got IOException", e);
        }
    }
}
