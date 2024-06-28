package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class SeasonParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public List<String> getParameterNames() {
        return Collections.singletonList("season");
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull final Context context, @NonNull String parameterName) {
        Calendar calendar = Calendar.getInstance();
        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        int leapYearModification = gregorianCalendar.isLeapYear(year) ? 1 : 0;

        // todo actually calculate the correct date
        int springStart = daysUntilMonth(3) + leapYearModification + 21;
        int summerStart = daysUntilMonth(6) + leapYearModification + 21;
        int autumnStart = daysUntilMonth(9) + leapYearModification + 21;
        int winterStart = daysUntilMonth(12) + leapYearModification + 21;

        final String result;
        if (day >= springStart && day < summerStart) {
            result = "spring";
        } else if (day >= summerStart && day < autumnStart) {
            result = "summer";
        } else if (day >= autumnStart && day < winterStart) {
            result = "autumn";
        } else {
            result = "winter";
        }

        return CompletableFuture.completedFuture(result);
    }

    @NonNull
    @Override
    public String getDescription(@NonNull final Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_season_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return null;
    }

    private int monthDays(int monthNumber) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.MONTH, monthNumber - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private int daysUntilMonth(int monthNumber) {
        int result = 0;
        for (int i = 1; i < monthNumber; ++i) {
            result += monthDays(i);
        }

        return result;
    }
}
