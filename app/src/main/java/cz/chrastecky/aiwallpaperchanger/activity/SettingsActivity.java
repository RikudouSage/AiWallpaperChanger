package cz.chrastecky.aiwallpaperchanger.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivitySettingsBinding;
import cz.chrastecky.aiwallpaperchanger.helper.ContentResolverHelper;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

public class SettingsActivity extends AppCompatActivity {

    private Uri directoryUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_settings);

        initializeForm();

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

        FloatingActionButton saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = new SharedPreferencesHelper().get(this).edit();

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

            editor.apply();
            finish();
        });

        binding.saveWallpapersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                activityLauncher.launch(intent);
            }
        });
    }

    private void initializeForm() {
        SharedPreferences preferences = new SharedPreferencesHelper().get(this);

        TextInputEditText apiKey = findViewById(R.id.api_key_field);
        apiKey.setText(preferences.getString(SharedPreferencesHelper.API_KEY, ""));

        if (
                preferences.contains(SharedPreferencesHelper.STORE_WALLPAPERS_URI)
                && ContentResolverHelper.canAccessDirectory(this, Uri.parse(preferences.getString(SharedPreferencesHelper.STORE_WALLPAPERS_URI, "")))
        ) {
            SwitchCompat storeWallpapers = findViewById(R.id.save_wallpapers_switch);
            storeWallpapers.setChecked(true);
        }
    }
}