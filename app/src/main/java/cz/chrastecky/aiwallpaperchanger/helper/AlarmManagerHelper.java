package cz.chrastecky.aiwallpaperchanger.helper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import cz.chrastecky.aiwallpaperchanger.background.AlarmReceiver;

public class AlarmManagerHelper {
    public static final int ALARM_REQUEST_CODE = 361;

    public static PendingIntent getAlarmIntent(Context context) {
        return getAlarmIntent(context, PendingIntent.FLAG_IMMUTABLE);
    }
    public static PendingIntent getAlarmIntent(Context context, int flags) {
        Intent intent = new Intent(AlarmReceiver.ACTION_NAME);
        intent.setClass(context, AlarmReceiver.class);

        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags);
    }
}
