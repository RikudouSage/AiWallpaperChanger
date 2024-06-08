package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.Future;

public interface PromptParameterProvider {
    @NonNull
    String getParameterName();
    @NonNull
    Future<String> getValue(final Context context);
}
