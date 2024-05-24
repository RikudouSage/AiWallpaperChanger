package cz.chrastecky.aiwallpaperchanger.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import cz.chrastecky.aiwallpaperchanger.helper.Logger;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_NAME = "cz.chrastecky.aiwallpaperchanger.ALARM_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(ACTION_NAME)) {
            return;
        }

        Logger logger = new Logger(context);

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> logger.error("UncaughtException", "Got uncaught exception", error)
        );

        logger.debug("AlarmReceiver", "Alarm intent received");
        WorkManager manager = WorkManager.getInstance(context);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GenerateAndSetBackgroundWorker.class)
                .setConstraints(constraints)
                .build();
        manager.enqueue(request);
        logger.debug("AlarmReceiver", "Worker enqueued");
    }
}
