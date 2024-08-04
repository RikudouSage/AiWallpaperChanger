package cz.chrastecky.aiwallpaperchanger.easymode;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.EasyModePrompt;
import cz.chrastecky.aiwallpaperchanger.dto.EasyModePromptParameter;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public final class EasyModePromptManager {
    private final EasyModeImageManager imageManager;
    @NonNull
    private final Context context;
    private final Logger logger;

    public EasyModePromptManager(@NonNull final Context context) {
        this.context = context;
        this.imageManager = new EasyModeImageManager(context);
        this.logger = new Logger(context);
    }

    @Nullable
    public List<EasyModePrompt> getPrompts() {
        try (InputStream source = context.getResources().openRawResource(R.raw.easy_mode_prompts)) {
            String text = new BufferedReader(new InputStreamReader(source)).lines().collect(Collectors.joining("\n"));
            return Arrays.stream(
                            new Gson().fromJson(text, EasyModePrompt[].class)
                    )
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("EasyModePromptManager", "Failed reading easy mode prompts file", e);
            return null;
        }
    }

    public CompletableFuture<List<EasyModePrompt>> getEnrichedPrompts(List<EasyModePrompt> prompts) {
        CompletableFuture<List<EasyModePrompt>> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final ArrayList<EasyModePrompt> result = new ArrayList<>();
            for (EasyModePrompt prompt : prompts) {
                final Map<String, List<EasyModePromptParameter>> targetParameterMap = new HashMap<>();

                for (String parameterName : prompt.getParameters().keySet()) {
                    final List<EasyModePromptParameter> targetParameters = new ArrayList<>();
                    final List<EasyModePromptParameter> parameters = prompt.getParameters().get(parameterName);
                    assert parameters != null;

                    for (EasyModePromptParameter parameter : parameters) {
                        try {
                            targetParameters.add(new EasyModePromptParameter(
                                    parameter.getName(),
                                    parameter.getValue(),
                                    imageManager.getImage("/" + prompt.getName() + "/" + parameterName + "/" + parameter.getName()).get()
                            ));
                        } catch (ExecutionException | InterruptedException e) {
                            logger.error("EasyModePromptManager", "Failed getting image for prompt parameter '" + parameter.getName() + "'", e.getCause());
                        }
                    }
                    targetParameterMap.put(parameterName, targetParameters);
                }
                try {
                    result.add(new EasyModePrompt(
                            prompt.getName(),
                            prompt.getPrompt(),
                            prompt.getFeatured(),
                            prompt.getTargetPrompt(),
                            targetParameterMap,
                            imageManager.getImage("/" + prompt.getName() + "/preview").get()
                    ));
                } catch (ExecutionException | InterruptedException e) {
                    logger.error("EasyModePromptManager", "Failed getting image for prompt '" + prompt.getName() + "'", e.getCause());
                }
            }

            future.complete(result);
        }, context);

        return future;
    }
}
