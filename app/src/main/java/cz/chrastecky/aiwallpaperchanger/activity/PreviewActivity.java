package cz.chrastecky.aiwallpaperchanger.activity;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Objects;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityPreviewBinding;

public class PreviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityResultLauncher<Intent> scheduleActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        finish();
                    }
                }
        );

        ActivityPreviewBinding binding = ActivityPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_generate_preview);

        Intent intent = getIntent();
        Bitmap image = BitmapFactory.decodeFile(Objects.requireNonNull(intent.getStringExtra("imagePath")));
        ImageView previewImage = findViewById(R.id.preview_image);
        previewImage.setImageBitmap(image);

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());

        Button okButton = findViewById(R.id.ok_button);
        okButton.setOnClickListener(view -> {
            SharedPreferences.Editor editor = new SharedPreferencesHelper().get(this).edit();
            editor.putString("generationParameters", intent.getStringExtra("generationParameters"));
            editor.apply();

            AsyncTask.execute(() -> {
                try {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                    wallpaperManager.setBitmap(image);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Intent configureIntent = new Intent(this, ConfigureScheduleActivity.class);
            intent.putExtra("generationParameters", intent.getStringExtra("generationParameters"));
            scheduleActivityLauncher.launch(configureIntent);
        });
    }
}