package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityUncaughtErrorBinding;

public class UncaughtErrorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityUncaughtErrorBinding binding = ActivityUncaughtErrorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_uncaught_exception);

        binding.aboutPageButton.setOnClickListener(view -> startActivity(new Intent(this, AboutActivity.class)));
    }
}