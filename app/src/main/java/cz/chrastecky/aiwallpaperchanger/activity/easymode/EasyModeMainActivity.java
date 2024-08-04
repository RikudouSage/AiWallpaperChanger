package cz.chrastecky.aiwallpaperchanger.activity.easymode;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityEasyModeMainBinding;
import cz.chrastecky.aiwallpaperchanger.dto.EasyModePrompt;
import cz.chrastecky.aiwallpaperchanger.easymode.EasyModePromptManager;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public class EasyModeMainActivity extends AppCompatActivity {
    private final Logger logger = new Logger(this);
    private final EasyModePromptManager promptManager = new EasyModePromptManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupExceptionLogging();

        ActivityEasyModeMainBinding binding = ActivityEasyModeMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        List<EasyModePrompt> prompts = promptManager.getEnrichedPrompts(promptManager.getPrompts()).join();
    }

    private void setupExceptionLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }

        ThreadHelper.setupGraphicalErrorHandler(logger, this);
    }
}