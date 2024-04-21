package cz.chrastecky.aiwallpaperchanger.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
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
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GenerateAndSetBackgroundWorker.class)
                .setConstraints(constraints)
                .build();
        manager.enqueue(request);
        Log.d("AlarmReceiver", "Worker enqueued");
    }
}
