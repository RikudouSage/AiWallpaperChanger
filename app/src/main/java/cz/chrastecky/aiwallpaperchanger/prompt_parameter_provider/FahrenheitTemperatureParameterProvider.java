package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class FahrenheitTemperatureParameterProvider extends AbstractTemperatureParameterProvider {
    @Override
    protected double convertToTargetUnit(double kelvin) {
        return Math.round((kelvin - 273.15) * 1.8 + 32);
    }

    @Override
    protected String getUnit() {
        return "°F";
    }

    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("temperature_f"));
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return "";
    }
}
