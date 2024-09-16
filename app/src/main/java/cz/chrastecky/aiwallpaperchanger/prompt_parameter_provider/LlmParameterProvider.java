package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PromptReplacer;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.aiwallpaperchanger.provider.AiTextProvider;
import cz.chrastecky.aiwallpaperchanger.provider.AiTextProviderCollection;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class LlmParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("llm"));
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull Context context, @NonNull String parameterName) {
        final Logger logger = new Logger(context);
        final String[] parts = parameterName.split(Pattern.quote(":"), 2);
        if (parts.length != 2 || !parts[0].equals("llm")) {
            logger.error("LLM Parameter", "Invalid LLM parameter: " + parameterName);
            return null;
        }

        final String prompt = parts[1];
        final CompletableFuture<String> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            PromptReplacer.replacePrompt(context, prompt, replacedPrompt -> {
                final AiTextProvider textProvider = new AiTextProviderCollection(context).getCurrentProvider();
                final String response = textProvider.getResponse(replacedPrompt).join();
                future.complete(response);
            });
        }, context);

        return future;
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_llm_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<List<String>> getParametersInText(@NonNull Context context, @NonNull String... texts) {
        final CompletableFuture<List<String>> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final List<String> result = new ArrayList<>();
            final Pattern regex = Pattern.compile("\\$\\{([^{}:]+)(?::((?:[^\\${}]|\\$\\{(?:[^\\${}]+)\\})*))?\\}");
            for (final String text : texts) {
                if (text == null) {
                    continue;
                }
                final Matcher matcher = regex.matcher(text);
                if (!matcher.find()) {
                    continue;
                }
                final String group = matcher.group(2);
                if (group == null) {
                    continue;
                }
                result.add("llm:" + group);
            }

            future.complete(result);
        }, context);

        return future;
    }
}
