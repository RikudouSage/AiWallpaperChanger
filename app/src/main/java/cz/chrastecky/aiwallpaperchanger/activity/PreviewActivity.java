package cz.chrastecky.aiwallpaperchanger.activity;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityPreviewBinding;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;
import cz.chrastecky.aiwallpaperchanger.helper.ContentResolverHelper;
import cz.chrastecky.aiwallpaperchanger.helper.History;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

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
            SharedPreferences preferences = new SharedPreferencesHelper().get(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("generationParameters", intent.getStringExtra("generationParameters"));
            editor.apply();

            AsyncTask.execute(() -> {
                try {
                    if (preferences.contains("storeWallpapersUri")) {
                        ContentResolverHelper.storeBitmap(this, Uri.parse(preferences.getString("storeWallpapersUri", "")), UUID.randomUUID() + ".png", image);
                    }

                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                    wallpaperManager.setBitmap(image);
                    editor.putString("lastChanged", DateFormat.getInstance().format(Calendar.getInstance().getTime()));
                    editor.apply();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            History history = new History(this);
            history.addItem(new StoredRequest(
                    UUID.randomUUID(),
                    new Gson().fromJson(intent.getStringExtra("generationParametersReplaced"), GenerateRequest.class),
                    Objects.requireNonNull(intent.getStringExtra("seed")),
                    Objects.requireNonNull(intent.getStringExtra("workerId")),
                    Objects.requireNonNull(intent.getStringExtra("workerName")),
                    new Date()
            ));

            Intent configureIntent = new Intent(this, ConfigureScheduleActivity.class);
            intent.putExtra("generationParameters", intent.getStringExtra("generationParameters"));
            scheduleActivityLauncher.launch(configureIntent);
        });
    }
}