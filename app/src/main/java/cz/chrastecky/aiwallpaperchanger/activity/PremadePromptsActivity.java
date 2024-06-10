package cz.chrastecky.aiwallpaperchanger.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityPremadePromptsBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.PremadePromptItemBinding;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;

public class PremadePromptsActivity extends AppCompatActivity {
    private final Logger logger = new Logger(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityPremadePromptsBinding binding = ActivityPremadePromptsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_premade_prompts);

        for (PremadePrompt prompt : getPrompts()) {
            PremadePromptItemBinding item = PremadePromptItemBinding.inflate(getLayoutInflater());
            item.setName(prompt.getName());

            binding.rootView.addView(item.getRoot());
        }
    }

    private PremadePrompt[] getPrompts() {
        try(InputStream source = getResources().openRawResource(R.raw.premade_prompts)) {
            String text = new BufferedReader(new InputStreamReader(source)).lines().collect(Collectors.joining("\n"));
            return new Gson().fromJson(text, PremadePrompt[].class);
        } catch (IOException e) {
            Toast.makeText(this, R.string.app_premade_prompts_failed_getting, Toast.LENGTH_LONG).show();
            logger.error("PremadePrompts", "Failed reading raw prompts file", e);
            return new PremadePrompt[] {};
        }
    }
}