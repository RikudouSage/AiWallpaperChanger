package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;

import java.io.File;
import java.util.Date;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityAboutBinding;
import cz.chrastecky.aiwallpaperchanger.sharing.AppFileProvider;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        binding.setVersion(BuildConfig.VERSION_NAME);
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_about);

        binding.aboutText.setMovementMethod(LinkMovementMethod.getInstance());

        binding.sendLogs.setOnClickListener(view -> {
            String date = DateFormat.format("yyyy-MM-dd", new Date()).toString();
            File logFile = new File(getFilesDir() + "/logs", "log." + date + ".txt");
            if (!logFile.exists()) {
                Toast.makeText(this, R.string.app_about_share_logs_no_log_file, Toast.LENGTH_LONG).show();
                return;
            }

            Uri imageUri = AppFileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".files_provider", logFile);

            Intent intent = new ShareCompat.IntentBuilder(this)
                    .setType("message/rfc822")
                    .setEmailTo(new String[]{BuildConfig.SUPPORT_EMAIL})
                    .setStream(imageUri)
                    .setSubject("AI Wallpaper Changer logs")
                    .setText("Hi there!\n\nI'm sending the logs for AI Wallpaper Changer attached.")
                    .setChooserTitle(getString(R.string.app_about_share_logs_chooser_title))
                    .createChooserIntent()
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        });
    }
}