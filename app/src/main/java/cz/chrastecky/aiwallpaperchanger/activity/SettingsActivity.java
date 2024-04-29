package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityHistoryBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivitySettingsBinding;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySettingsBinding binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_settings);

        initializeForm();

        FloatingActionButton saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = new SharedPreferencesHelper().get(this).edit();

            TextInputEditText apiKeyField = findViewById(R.id.api_key_field);
            if (apiKeyField.getText() == null) {
                editor.remove("api_key");
                editor.apply();
                finish();
                return;
            }

            String apiKey = apiKeyField.getText().toString();
            if (apiKey.isEmpty()) {
                editor.remove("api_key");
                editor.apply();
                finish();
                return;
            }

            editor.putString("api_key", apiKey);
            editor.apply();
            finish();
        });
    }

    private void initializeForm() {
        SharedPreferences preferences = new SharedPreferencesHelper().get(this);

        TextInputEditText apiKey = findViewById(R.id.api_key_field);
        apiKey.setText(preferences.getString("api_key", ""));
    }
}