package cz.chrastecky.aiwallpaperchanger.helper;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;

public class PermissionHelper {
    public static boolean shouldCheckForPermissions(Context context) {
        PendingIntent pendingIntent = AlarmManagerHelper.getAlarmIntent(context, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    public static void askForDataSaverException(Context context) {
        final String doNotAskAgainKey = "data_saver_exception_do_not_ask";
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);

//        SharedPreferences.Editor ed = preferences.edit();
//        ed.remove(doNotAskAgainKey);
//        ed.apply();

        if (preferences.contains(doNotAskAgainKey)) {
            return;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        int status =  connectivityManager.getRestrictBackgroundStatus();

        if (status != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.app_permission_data_saver_dialog_body);
        builder.setTitle(R.string.app_permission_data_saver_dialog_title);
        builder.setPositiveButton(R.string.app_permission_dialog_button_exception, (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
            context.startActivity(intent);
        });
        builder.setNeutralButton(R.string.app_permission_dialog_button_ignore, (dialog, which) -> {});
        builder.setNegativeButton(R.string.app_permission_dialog_button_dont_ask_again, (dialog, which) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(doNotAskAgainKey, true);
            editor.apply();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void askForDozeModeException(Context context) {
        final String doNotAskAgainKey = "doze_mode_exception_do_not_ask";
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);

        if (preferences.contains(doNotAskAgainKey)) {
            return;
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(context.getPackageName())) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.app_permissions_doze_mode_dialog_body);
        builder.setTitle(R.string.app_permissions_doze_mode_dialog_title);
        builder.setPositiveButton(R.string.app_permission_dialog_button_exception, (dialog, which) -> {
            if (BuildConfig.DOZE_MANAGEMENT_ENABLED) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                context.startActivity(intent);
            } else {
                context.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        });
        builder.setNeutralButton(R.string.app_permission_dialog_button_ignore, (dialog, which) -> {});
        builder.setNegativeButton(R.string.app_permission_dialog_button_dont_ask_again, (dialog, which) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(doNotAskAgainKey, true);
            editor.apply();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
