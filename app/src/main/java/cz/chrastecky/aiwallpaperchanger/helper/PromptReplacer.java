package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.PromptParameterProviders;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider.PromptParameterProvider;

public class PromptReplacer {
    @Nullable
    public static String replacePrompt(@NonNull Context context, @NonNull String prompt) {
        return replacePrompt(context, prompt, false);
    }

    @Nullable
    public static String replacePrompt(@NonNull Context context, @NonNull String prompt, boolean showToast) {
        if (!prompt.contains("${")) {
            return prompt;
        }
        final Logger logger = new Logger(context);
        final List<PromptParameterProvider> providers = new PromptParameterProviders().getProviders();
        final List<String> values;
        try {
            values = CompletableFuture.supplyAsync(() -> FutureResolver.resolveFutures(
                    providers.stream()
                            .map(provider -> provider.getValue(context))
                            .collect(Collectors.toList()),
                    logger
            )).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < providers.size(); ++i) {
            PromptParameterProvider provider = providers.get(i);
            @Nullable String value = values.get(i);

            if (!prompt.contains("${" + provider.getParameterName() + "}")) {
                continue;
            }

            if (value == null) {
                if (showToast) {
                    Toast.makeText(context, context.getString(R.string.app_error_prompt_parameter_invalid, provider.getParameterName()), Toast.LENGTH_LONG).show();
                }
                return null;
            }
            prompt = prompt.replace("${" + provider.getParameterName() + "}", value);
        }

        return prompt;
    }
}
