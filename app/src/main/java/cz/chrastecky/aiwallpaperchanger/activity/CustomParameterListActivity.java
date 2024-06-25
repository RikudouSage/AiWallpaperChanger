package cz.chrastecky.aiwallpaperchanger.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityCustomParameterListBinding;

public class CustomParameterListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCustomParameterListBinding binding = ActivityCustomParameterListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_settings_custom_parameters);
    }
}