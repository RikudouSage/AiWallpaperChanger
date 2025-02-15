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
public class MoonPhaseParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("moon_phase"));
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull Context context, @NonNull String parameterName) {
        final Calendar now = Calendar.getInstance();

        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        final int day = now.get(Calendar.DAY_OF_MONTH);

        if (month < 3) {
            year--;
            month += 12;
        }

        final int century = year / 100;
        final int centuryCorrection = 2 - century + (century / 4);
        final int daysSinceEpoch = (int) (365.25 * (year + 4716)) + (int) (30.6001 * (month + 1)) + day + centuryCorrection - 1524;

        final double daysSinceKnownNewMoon = daysSinceEpoch - 2451550.1; // Reference new moon date (Jan 6, 2000)
        final double lunarCycles = daysSinceKnownNewMoon / 29.53058867; // Length of lunar cycle
        final double phaseFraction = lunarCycles - Math.floor(lunarCycles);

        final String result;

        if (phaseFraction < 0.03 || phaseFraction > 0.97) {
            result = "New Moon";
        } else if (phaseFraction < 0.23) {
            result = "Waxing Crescent";
        } else if (phaseFraction < 0.27) {
            result = "First Quarter";
        } else if (phaseFraction < 0.48) {
            result = "Waxing Gibbous";
        } else if (phaseFraction < 0.52) {
            result = "Full Moon";
        } else if (phaseFraction < 0.73) {
            result = "Waning Gibbous";
        } else if (phaseFraction < 0.77) {
            result = "Last Quarter";
        } else {
            result = "Waning Crescent";
        }

        return CompletableFuture.completedFuture(result);
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_moon_phase);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return Collections.emptyList();
    }
}
