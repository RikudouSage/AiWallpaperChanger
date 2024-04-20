package cz.chrastecky.aiwallpaperchanger.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String ACTION_NAME = "cz.chrastecky.aiwallpaperchanger.ALARM_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals(ACTION_NAME)) {
            return;
        }

        Log.d("AlarmReceiver", "Alarm intent received");
        WorkManager manager = WorkManager.getInstance(context);
        manager.enqueue(new OneTimeWorkRequest.Builder(GenerateAndSetBackgroundWorker.class).build());
        Log.d("AlarmReceiver", "Worker enqueued");
    }
}
