package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.PromptParameterProviders;
import cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider.PromptParameterProvider;

public class PromptReplacer {
    public interface OnPromptReplaced {
        void onReplaced(@Nullable String prompt);
    }

    private static final PromptParameterProviders parameterProviders = new PromptParameterProviders();

    public static void replacePrompt(@NonNull Context context, @NonNull String prompt, OnPromptReplaced onPromptReplaced) {
        if (!prompt.contains("${")) {
            onPromptReplaced.onReplaced(prompt);
            return;
        }

        new Thread(() -> {
            ThreadHelper.setupErrorHandler(new Logger(context));

            String promptCopy = prompt;
            final List<PromptParameterProvider> providers = parameterProviders.getProviders();

            for (int i = 0; i < providers.size(); ++i) {
                PromptParameterProvider provider = providers.get(i);
                for (String parameterName : provider.getParameterNames(context).join()) {
                    if (!promptCopy.contains("${" + parameterName + "}")) {
                        continue;
                    }
                    CompletableFuture<String> value = provider.getValue(context, parameterName);
                    if (value == null) {
                        onPromptReplaced.onReplaced(null);
                        return;
                    }

                    promptCopy = promptCopy.replace("${" + parameterName + "}", value.join());
                }
            }

            onPromptReplaced.onReplaced(promptCopy);
        }).start();
    }
}
