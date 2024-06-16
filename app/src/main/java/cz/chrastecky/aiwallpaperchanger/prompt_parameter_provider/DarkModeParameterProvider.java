package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class DarkModeParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public String getParameterName() {
        return "bg";
    }

    @Nullable
    @Override
    public Future<String> getValue(@NonNull Context context) {
        final String result;
        if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            result = "dark";
        } else {
            result = "light";
        }

        return CompletableFuture.completedFuture(result);
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context) {
        return context.getString(R.string.app_parameter_dark_mode_description);
    }
}