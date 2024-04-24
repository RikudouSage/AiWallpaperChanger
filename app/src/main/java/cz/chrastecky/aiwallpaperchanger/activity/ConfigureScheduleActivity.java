package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityConfigureScheduleBinding;
import cz.chrastecky.aiwallpaperchanger.helper.AlarmManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ChannelHelper;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

public class ConfigureScheduleActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 361;
    private static final int PERMISSION_REQUEST_CODE = 877;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityConfigureScheduleBinding binding = ActivityConfigureScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_configure_schedule);

        Spinner timesSelect = findViewById(R.id.times_select);
        timesSelect.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, Arrays.asList(1, 2, 4, 6, 12, 24, 48)));

        Button scheduleButton = findViewById(R.id.schedule_button);
        scheduleButton.setOnClickListener(view -> {
            handleDozeMode();
            schedule();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//                    ChannelHelper.createChannels(this);
//                    schedule();
//                } else {
//                    SharedPreferences sharedPreferences = new SharedPreferencesHelper().get(this);
//                    int deniedCount = sharedPreferences.getInt("deniedCount", 0);
//
//                    if (deniedCount >= 2) {
//                        Toast.makeText(this, R.string.app_error_notification_permission, Toast.LENGTH_LONG).show();
//                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                        Uri uri = Uri.fromParts("package", getPackageName(), null);
//                        intent.setData(uri);
//                        startActivity(intent);
//                    } else {
//                        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
//                    }
//                }
//            } else {
//                ChannelHelper.createChannels(this);
//                schedule();
//            }
        });
    }

    private void handleDozeMode() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            return;
        }
        if (BuildConfig.DOZE_MANAGEMENT_ENABLED) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.notification_doze_mode_disable, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ChannelHelper.createChannels(this);
                schedule();
            } else {
                SharedPreferences sharedPreferences = new SharedPreferencesHelper().get(this);
                int deniedCount = sharedPreferences.getInt("deniedCount", 0);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("deniedCount", ++deniedCount);
                editor.apply();

                Toast.makeText(this, R.string.app_error_notification_permission, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void schedule() {
        Spinner timesSelect = findViewById(R.id.times_select);

        SharedPreferences.Editor sharedPreferences = new SharedPreferencesHelper().get(this).edit();
        sharedPreferences.putInt("selectedInterval", (Integer) timesSelect.getSelectedItem());
        sharedPreferences.apply();

        AlarmManagerHelper.scheduleAlarm(this);

        Toast.makeText(this, R.string.app_success_scheduled, Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }
}