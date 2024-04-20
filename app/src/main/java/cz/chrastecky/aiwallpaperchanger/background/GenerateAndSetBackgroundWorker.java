package cz.chrastecky.aiwallpaperchanger.background;

import android.app.Notification;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.io.IOException;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.helper.ChannelHelper;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.horde.AiHorde;

public class GenerateAndSetBackgroundWorker extends Worker {
    private static final int NOTIFICATION_ID = 611;

    public GenerateAndSetBackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("WorkerJob", "Inside doWork()");
        AiHorde aiHorde = new AiHorde(Volley.newRequestQueue(getApplicationContext()));
        SharedPreferences preferences = new SharedPreferencesHelper().get(getApplicationContext());

        if (!preferences.contains("generationParameters")) {
            return Result.failure();
        }

        setForegroundAsync(createForegroundInfo());
        GenerateRequest request = new Gson().fromJson(preferences.getString("generationParameters", ""), GenerateRequest.class);
        aiHorde.generateImage(request, status -> {
            setForegroundAsync(createForegroundInfo());
        }, response -> {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
            try {
                wallpaperManager.setBitmap(response);
            } catch (IOException e) {
                Log.e("AIWallpaperError", "Failed setting new wallpaper", e);
            }
        }, error -> {
            Log.e("AIWallpaperError", "Failed generating AI image", error);
        });

        return Result.success();
    }

    private ForegroundInfo createForegroundInfo() {
        Context context = getApplicationContext();
        String id = context.getString(R.string.notification_channel_background_work_id);

        ChannelHelper.createChannels(getApplicationContext());

        Notification notification = new NotificationCompat.Builder(context, id)
                .setContentTitle(context.getString(R.string.notification_background_work_title))
                .setTicker(context.getString(R.string.notification_background_work_title))
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            return new ForegroundInfo(NOTIFICATION_ID, notification);
        }
    }
}
