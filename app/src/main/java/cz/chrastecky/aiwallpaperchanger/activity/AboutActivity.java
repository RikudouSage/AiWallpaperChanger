package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityAboutBinding;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
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
            createLogCatFile();
            File logcatFile = new File(getFilesDir() + "/logs", "logcat.log");

            ArrayList<Uri> attachments = new ArrayList<>();
            attachments.add(AppFileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".files_provider", logFile));
            if (logcatFile.exists()) {
                attachments.add(AppFileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".files_provider", logcatFile));
            }

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("message/rfc822");
            intent.putExtra(Intent.EXTRA_SUBJECT, "AI Wallpaper Changer logs");
            intent.putExtra(Intent.EXTRA_TEXT, "Hi there!\n\nI'm sending the logs for AI Wallpaper Changer attached.");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildConfig.SUPPORT_EMAIL});
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
            intent = Intent.createChooser(intent, getString(R.string.app_about_share_logs_chooser_title));

            startActivity(intent);
        });
    }

    private void createLogCatFile() {
        final String cmd = "logcat -d -v time";
        InputStreamReader reader = null;

        File logcat = new File(getFilesDir() + "/logs/logcat.log");
        if (logcat.exists()) {
            logcat.delete();
        }

        try (FileWriter writer = new FileWriter(logcat)) {
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());
            writer.write ("App version: " + BuildConfig.VERSION_NAME + "\n");

            char[] buffer = new char[10000];
            do {
                int charsRead = reader.read(buffer, 0, buffer.length);
                if (charsRead == -1) {
                    break;
                }
                writer.write (buffer, 0, charsRead);
            } while (true);

            reader.close();
        } catch (IOException e) {
            final Logger logger = new Logger(this);
            logger.error("LogSending", "Failed creating logcat logs", e);
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    logger.error("LogSending", "Failed closing reader", e);
                }
            }
        }
    }
}