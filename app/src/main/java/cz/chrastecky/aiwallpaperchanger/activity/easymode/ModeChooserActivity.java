package cz.chrastecky.aiwallpaperchanger.activity.easymode;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityModeChooserBinding;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public class ModeChooserActivity extends AppCompatActivity {
    private final Logger logger = new Logger(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupExceptionLogging();

        ActivityModeChooserBinding binding = ActivityModeChooserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void setupExceptionLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }

        ThreadHelper.setupGraphicalErrorHandler(logger, this);
    }
}