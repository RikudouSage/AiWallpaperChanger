package cz.chrastecky.aiwallpaperchanger.helper;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

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

    public static void scheduleAlarm(Context context) {
        SharedPreferences sharedPreferences = new SharedPreferencesHelper().get(context);
        if (!sharedPreferences.contains("selectedInterval")) {
            throw new RuntimeException("Selected interval must be populated when scheduling alarm");
        }
        int interval = sharedPreferences.getInt("selectedInterval", 0);

        PendingIntent intent = getAlarmIntent(context);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
//        calendar.setTimeInMillis(calendar.getTimeInMillis() + 60_000);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(AlarmManagerHelper.getAlarmIntent(context, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE));
        alarmManager.setRepeating(
                AlarmManager.RTC,
                calendar.getTimeInMillis(),
                (long) ((24.0 / interval) * 60 * 60 * 1_000),
                intent
        );
//        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), intent);
//        Log.d("Schedule", "Should run at " + calendar.getTime());
    }
}
