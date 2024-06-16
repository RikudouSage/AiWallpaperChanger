package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;

public class PremadePromptHelper {
    public static PremadePrompt[] getPrompts(Context context) throws IOException {
        try (InputStream source = context.getResources().openRawResource(R.raw.premade_prompts)) {
            String text = new BufferedReader(new InputStreamReader(source)).lines().collect(Collectors.joining("\n"));
            return Arrays.stream(
                    new Gson().fromJson(text, PremadePrompt[].class)
            )
                    .sorted(Comparator.comparing(PremadePrompt::getName))
                    .toArray(PremadePrompt[]::new);
        }
    }

    @Nullable
    public static PremadePrompt findByName(final Context context, @NonNull final String name) throws IOException {
        return Arrays.stream(PremadePromptHelper.getPrompts(context))
                .filter(premadePrompt -> premadePrompt.getName().equals(name))
                .findFirst().orElse(null);
    }
}
