package cz.chrastecky.aiwallpaperchanger.helper;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

        Calendar target = Calendar.getInstance();
        double howOften = 24.0 / interval;
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        int howOftenMilliseconds = (int) (howOften * 60 * 60 * 1_000);

        Calendar now = Calendar.getInstance();
        while (!target.after(now)) {
            target.setTimeInMillis(target.getTimeInMillis() + howOftenMilliseconds);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(AlarmManagerHelper.getAlarmIntent(context, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE));
        alarmManager.setRepeating(
                AlarmManager.RTC,
                target.getTimeInMillis(),
                (long) ((24.0 / interval) * 60 * 60 * 1_000),
                intent
        );

        ShortcutManagerHelper.createShortcuts(context);

//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
//        Calendar now = Calendar.getInstance();
//        now.add(Calendar.MINUTE, 1);
//        alarmManager.set(AlarmManager.RTC, now.getTimeInMillis(), getAlarmIntent(context));
//        Logger.debug("Schedule", "Should run at " + now.getTime());
    }
}
