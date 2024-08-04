package cz.chrastecky.aiwallpaperchanger.activity.easymode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.activity.MainActivity;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityModeChooserBinding;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public class ModeChooserActivity extends AppCompatActivity {
    private final Logger logger = new Logger(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupExceptionLogging();

        ActivityModeChooserBinding binding = ActivityModeChooserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences.Editor preferences = SharedPreferencesHelper.get(this).edit();

        binding.advancedModeButton.setOnClickListener(view -> {
            preferences.putString(SharedPreferencesHelper.APP_MODE, "advanced");
            preferences.apply();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
            finish();
        });

        binding.easyModeButton.setOnClickListener(view -> {
            preferences.putString(SharedPreferencesHelper.APP_MODE, "easy");
            preferences.apply();

            Intent intent = new Intent(this, EasyModeMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            startActivity(intent);
            finish();
        });
    }

    private void setupExceptionLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }

        ThreadHelper.setupGraphicalErrorHandler(logger, this);
    }
}