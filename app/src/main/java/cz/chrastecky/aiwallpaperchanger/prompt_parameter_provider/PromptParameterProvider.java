package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Future;

public interface PromptParameterProvider {
    @NonNull
    String getParameterName();
    @Nullable
    Future<String> getValue(@NonNull final Context context);
    @NonNull
    String getDescription(@NonNull final Context context);
}
