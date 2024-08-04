package cz.chrastecky.aiwallpaperchanger.activity;

import android.annotation.SuppressLint;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.action.LiveWallpaperAction;
import cz.chrastecky.aiwallpaperchanger.action.StaticWallpaperAction;
import cz.chrastecky.aiwallpaperchanger.activity.easymode.EasyModeMainActivity;
import cz.chrastecky.aiwallpaperchanger.background.LiveWallpaperService;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivitySettingsBinding;
import cz.chrastecky.aiwallpaperchanger.helper.ContentResolverHelper;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

public class SettingsActivity extends AppCompatActivity {

    private Uri directoryUri = null;
    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_settings);

        initializeForm(binding);

        @SuppressLint("WrongConstant")
        ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        assert result.getData() != null;
                        assert result.getData().getData() != null;
                        Uri uri = result.getData().getData();

                        final int takeFlags = result.getData().getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);

                        directoryUri = uri;
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        binding.saveWallpapersSwitch.setChecked(false);
                    }
                }
        );

        binding.useLiveWallpaper.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isLiveWallpaper()) {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(this, LiveWallpaperService.class));
                startActivity(intent);
            }
        });

        FloatingActionButton saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = SharedPreferencesHelper.get(this).edit();

            TextInputEditText apiKeyField = findViewById(R.id.api_key_field);
            if (apiKeyField.getText() == null) {
                editor.remove(SharedPreferencesHelper.API_KEY);
            } else {
                String apiKey = apiKeyField.getText().toString();
                if (apiKey.isEmpty()) {
                    editor.remove(SharedPreferencesHelper.API_KEY);
                } else {
                    editor.putString(SharedPreferencesHelper.API_KEY, apiKey);
                }
            }


            if (directoryUri == null) {
                editor.remove(SharedPreferencesHelper.STORE_WALLPAPERS_URI);
            } else {
                editor.putString(SharedPreferencesHelper.STORE_WALLPAPERS_URI, directoryUri.toString());
            }

            if (binding.useLiveWallpaper.isChecked()) {
                editor.putString(SharedPreferencesHelper.WALLPAPER_ACTION, LiveWallpaperAction.ID);
            } else {
                editor.putString(SharedPreferencesHelper.WALLPAPER_ACTION, StaticWallpaperAction.ID);
            }

            editor.putBoolean(SharedPreferencesHelper.ALLOW_LARGE_NUMERIC_VALUES, binding.allowLargeValues.isChecked());
            editor.putString(SharedPreferencesHelper.APP_MODE, binding.enableEasyMode.isChecked() ? "easy" : "advanced");

            editor.apply();

            if (binding.enableEasyMode.isChecked()) {
                Intent easyModeIntent = new Intent(this, EasyModeMainActivity.class);
                easyModeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                startActivity(easyModeIntent);
            }

            finish();
        });

        binding.saveWallpapersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                activityLauncher.launch(intent);
            }
        });

        binding.toolbar.getOverflowIcon().setColorFilter(getColor(R.color.md_theme_onPrimary), PorterDuff.Mode.SRC_ATOP);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.custom_parameters_menu_item) {
                startActivity(new Intent(this, CustomParameterListActivity.class));
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.useLiveWallpaper.isChecked() && !isLiveWallpaper()) {
            binding.useLiveWallpaper.setChecked(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.settings_menu, menu);

        return true;
    }

    private void initializeForm(ActivitySettingsBinding binding) {
        SharedPreferences preferences = SharedPreferencesHelper.get(this);

        binding.apiKeyField.setText(preferences.getString(SharedPreferencesHelper.API_KEY, ""));
        binding.allowLargeValues.setChecked(preferences.getBoolean(SharedPreferencesHelper.ALLOW_LARGE_NUMERIC_VALUES, false));

        if (
                preferences.contains(SharedPreferencesHelper.STORE_WALLPAPERS_URI)
                && ContentResolverHelper.canAccessDirectory(this, Uri.parse(preferences.getString(SharedPreferencesHelper.STORE_WALLPAPERS_URI, "")))
        ) {
            binding.saveWallpapersSwitch.setChecked(true);
        }

        if (preferences.getString(SharedPreferencesHelper.WALLPAPER_ACTION, StaticWallpaperAction.ID).equals(LiveWallpaperAction.ID) && isLiveWallpaper()) {
            binding.useLiveWallpaper.setChecked(true);
        }
    }

    private boolean isLiveWallpaper() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
        WallpaperInfo currentWallpaper = wallpaperManager.getWallpaperInfo();
        if (currentWallpaper == null) {
            return false;
        }

        return currentWallpaper.getComponent().equals(new ComponentName(this, LiveWallpaperService.class));
    }
}