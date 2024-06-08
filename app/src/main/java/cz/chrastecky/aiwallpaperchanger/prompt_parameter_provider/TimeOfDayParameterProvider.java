package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class TimeOfDayParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public String getParameterName() {
        return "tod";
    }

    @NonNull
    @Override
    public Future<String> getValue(final Context context) {
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
}