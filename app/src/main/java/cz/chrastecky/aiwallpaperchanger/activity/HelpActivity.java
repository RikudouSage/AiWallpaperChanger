package cz.chrastecky.aiwallpaperchanger.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.PromptParameterProviders;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityHelpBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.PromptParameterDescriptionItemBinding;
import cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider.PromptParameterProvider;

public class HelpActivity extends AppCompatActivity {
    private final PromptParameterProviders providers = new PromptParameterProviders();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityHelpBinding binding = ActivityHelpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_help);

        for (final PromptParameterProvider provider : providers.getProviders()) {
            for (String parameterName : provider.getParameterNames(this).join()) {
                PromptParameterDescriptionItemBinding template = PromptParameterDescriptionItemBinding.inflate(getLayoutInflater());
                template.setProvider(provider);
                template.setParameterName(parameterName);
                binding.parameterProvidersRoot.addView(template.getRoot());
            }
        }
    }
}