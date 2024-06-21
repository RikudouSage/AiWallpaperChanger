package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityConfigureScheduleBinding;
import cz.chrastecky.aiwallpaperchanger.helper.AlarmManagerHelper;
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
        scheduleButton.setOnClickListener(view -> schedule());
    }

    private void schedule() {
        Spinner timesSelect = findViewById(R.id.times_select);

        SharedPreferences.Editor sharedPreferences = new SharedPreferencesHelper().get(this).edit();
        sharedPreferences.putInt(SharedPreferencesHelper.CONFIGURED_SCHEDULE_INTERVAL, (Integer) timesSelect.getSelectedItem());
        sharedPreferences.apply();

        AlarmManagerHelper.scheduleAlarm(this);

        Toast.makeText(this, R.string.app_success_scheduled, Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }
}