package cz.chrastecky.aiwallpaperchanger.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.background.AlarmReceiver;
import cz.chrastecky.aiwallpaperchanger.helper.AlarmManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ShortcutManagerHelper;

public class TriggerNextImageActivity extends AppCompatActivity {
    public static final String ACTION_NAME = BuildConfig.APPLICATION_ID + ".GENERATE_NEXT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PendingIntent pendingIntent = AlarmManagerHelper.getAlarmIntent(this, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            Intent intent = new Intent(AlarmReceiver.ACTION_NAME)
                    .setPackage(BuildConfig.APPLICATION_ID);
            sendBroadcast(intent);

            Toast.makeText(this, R.string.app_shortcut_next_image_info, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.app_shortcut_next_image_error, Toast.LENGTH_LONG).show();
            ShortcutManagerHelper.hideShortcuts(this);
        }

        finish();
    }
}