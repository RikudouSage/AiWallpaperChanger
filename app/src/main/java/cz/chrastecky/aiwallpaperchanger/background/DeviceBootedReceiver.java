package cz.chrastecky.aiwallpaperchanger.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import cz.chrastecky.aiwallpaperchanger.helper.AlarmManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

public class DeviceBootedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null || !intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            return;
        }

        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
        if (!preferences.contains("selectedInterval")) {
            return;
        }

        AlarmManagerHelper.scheduleAlarm(context);
    }
}
