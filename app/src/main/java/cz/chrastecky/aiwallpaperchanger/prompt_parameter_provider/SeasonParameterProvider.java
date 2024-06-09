package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class SeasonParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public String getParameterName() {
        return "season";
    }

    @Nullable
    @Override
    public Future<String> getValue(Context context) {
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
