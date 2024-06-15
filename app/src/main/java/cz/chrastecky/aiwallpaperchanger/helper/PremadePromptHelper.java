package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;

public class PremadePromptHelper {
    public static PremadePrompt[] getPrompts(Context context) throws IOException {
        try (InputStream source = context.getResources().openRawResource(R.raw.premade_prompts)) {
            String text = new BufferedReader(new InputStreamReader(source)).lines().collect(Collectors.joining("\n"));
            return new Gson().fromJson(text, PremadePrompt[].class);
        }
    }
}
