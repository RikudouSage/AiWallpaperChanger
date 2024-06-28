package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class TimeOfDayParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public List<String> getParameterNames() {
        return Collections.singletonList("tod");
    }

    @NonNull
    @Override
    public CompletableFuture<String> getValue(@NonNull final Context context, @NonNull String parameterName) {
        final Calendar calendar = Calendar.getInstance();
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);
        final String result;

        if (hour == 0) {
            result = "midnight";
        } else if (hour >= 21 || hour <= 4) {
            result = "night";
        } else if (hour < 12) {
            result = "morning";
        } else if (hour == 12) {
            result = "noon";
        } else if (hour < 17) {
            result = "afternoon";
        } else {
            result = "evening";
        }
        return CompletableFuture.completedFuture(result);
    }

    @NonNull
    @Override
    public String getDescription(@NonNull final Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_time_of_day_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return null;
    }
}
