package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.data.AppDatabase;
import cz.chrastecky.aiwallpaperchanger.data.entity.SavedPrompt;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityPremadePromptsBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.PremadePromptItemBinding;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;
import cz.chrastecky.aiwallpaperchanger.helper.DatabaseHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PremadePromptHelper;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public class PremadePromptsActivity extends AppCompatActivity {
    private static final int POSITION_PREMADE_PROMPTS = 0;
    private static final int POSITION_SAVED_PROMPTS = 1;

    private final Logger logger = new Logger(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences preferences = new SharedPreferencesHelper().get(this);

        final ActivityPremadePromptsBinding binding = ActivityPremadePromptsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_premade_prompts);

        final TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(final TabLayout.Tab tab) {
                // todo add loader
                ThreadHelper.runInThread(() -> {
                    final PremadePrompt[] prompts;
                    final SharedPreferences.Editor editor = preferences.edit();

                    if (tab.getPosition() == POSITION_PREMADE_PROMPTS) {
                        prompts = getPremadePrompts();
                        runOnUiThread(() -> setTitle(R.string.app_title_premade_prompts));
                        editor.putInt(SharedPreferencesHelper.LAST_VISITED_PROMPT_TYPE, POSITION_PREMADE_PROMPTS);
                    } else {
                        prompts = getSavedPrompts();
                        runOnUiThread(() -> setTitle(R.string.app_title_saved_prompts));
                        editor.putInt(SharedPreferencesHelper.LAST_VISITED_PROMPT_TYPE, POSITION_SAVED_PROMPTS);
                    }
                    editor.apply();

                    runOnUiThread(() -> {
                        for (PremadePrompt prompt : prompts) {
                            PremadePromptItemBinding item = PremadePromptItemBinding.inflate(getLayoutInflater());
                            item.setItem(prompt);

                            binding.rootView.addView(item.getRoot());
                        }
                    });
                }, PremadePromptsActivity.this);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                binding.rootView.removeAllViews();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });

        tabLayout.selectTab(tabLayout.getTabAt(
                preferences.getInt(SharedPreferencesHelper.LAST_VISITED_PROMPT_TYPE, POSITION_PREMADE_PROMPTS)
        ));
    }

    private PremadePrompt[] getPremadePrompts() {
        try {
            return PremadePromptHelper.getPrompts(this);
        } catch (IOException e) {
            Toast.makeText(this, R.string.app_premade_prompts_failed_getting, Toast.LENGTH_LONG).show();
            logger.error("PremadePrompts", "Failed reading raw prompts file", e);
            return new PremadePrompt[] {};
        }
    }

    @NonNull
    private PremadePrompt[] getSavedPrompts() {
        final List<PremadePrompt> result = new ArrayList<>();

        final AppDatabase database = DatabaseHelper.getDatabase(this);
        final List<SavedPrompt> prompts = database.savedPrompts().getAll();

        for (final SavedPrompt savedPrompt : prompts) {
            result.add(PremadePromptHelper.convertSavedPrompt(savedPrompt));
        }

        return result.toArray(new PremadePrompt[] {});
    }
}