package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.List;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityHistoryBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.HistoryItemBinding;
import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;
import cz.chrastecky.aiwallpaperchanger.helper.History;

public class HistoryActivity extends AppCompatActivity {

    private final History history = new History(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityHistoryBinding binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_history);

        List<StoredRequest> history = this.history.getHistory().stream().limit(96).collect(Collectors.toList());
        if (history.isEmpty()) {
            binding.noHistoryText.setVisibility(View.VISIBLE);
            return;
        }

        for (StoredRequest request : history) {
            String json = new Gson().toJson(request);
            HistoryItemBinding template = HistoryItemBinding.inflate(getLayoutInflater());
            template.setDate(DateFormat.getInstance().format(request.getCreated()));
            template.setRequest(json);
            template.hordeNgButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://horde-ng.org/generate?request=" + Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)));
                startActivity(intent);
            });
            binding.resultWrapper.addView(template.getRoot());
        }
    }
}