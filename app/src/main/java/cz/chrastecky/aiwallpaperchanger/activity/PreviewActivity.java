package cz.chrastecky.aiwallpaperchanger.activity;

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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.action.StaticWallpaperAction;
import cz.chrastecky.aiwallpaperchanger.action.WallpaperAction;
import cz.chrastecky.aiwallpaperchanger.action.WallpaperActionCollection;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityPreviewBinding;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;
import cz.chrastecky.aiwallpaperchanger.helper.ContentResolverHelper;
import cz.chrastecky.aiwallpaperchanger.helper.History;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;

public class PreviewActivity extends AppCompatActivity {
    private final WallpaperActionCollection wallpaperActionCollection = new WallpaperActionCollection();
    private final Logger logger = new Logger(this);

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
            editor.putString(SharedPreferencesHelper.STORED_GENERATION_PARAMETERS, intent.getStringExtra("generationParameters"));
            editor.apply();

            AsyncTask.execute(() -> {
                if (preferences.contains(SharedPreferencesHelper.STORE_WALLPAPERS_URI)) {
                    ContentResolverHelper.storeBitmap(this, Uri.parse(preferences.getString(SharedPreferencesHelper.STORE_WALLPAPERS_URI, "")), UUID.randomUUID() + ".png", image);
                }

                WallpaperAction wallpaperAction = wallpaperActionCollection.findById(
                        preferences.getString(SharedPreferencesHelper.WALLPAPER_ACTION, StaticWallpaperAction.ID)
                );
                if (!wallpaperAction.setWallpaper(this, image)) {
                    logger.error("Preview", "Failed setting the wallpaper");
                }

                editor.putString(SharedPreferencesHelper.WALLPAPER_LAST_CHANGED, DateFormat.getInstance().format(Calendar.getInstance().getTime()));
                editor.apply();
            });

            History history = new History(this);
            history.addItem(new StoredRequest(
                    UUID.randomUUID(),
                    new Gson().fromJson(intent.getStringExtra("generationParametersReplaced"), GenerateRequest.class),
                    Objects.requireNonNull(intent.getStringExtra("seed")),
                    Objects.requireNonNull(intent.getStringExtra("workerId")),
                    Objects.requireNonNull(intent.getStringExtra("workerName")),
                    new Date(),
                    intent.getStringExtra("model")
            ));

            Intent configureIntent = new Intent(this, ConfigureScheduleActivity.class);
            intent.putExtra("generationParameters", intent.getStringExtra("generationParameters"));
            scheduleActivityLauncher.launch(configureIntent);
        });
    }
}