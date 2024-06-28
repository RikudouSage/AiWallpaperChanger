package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class BatteryLevelParameterProvider implements PromptParameterProvider {
    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("battery"));
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull Context context, @NonNull String parameterName) {
        final Logger logger = new Logger(context);

        final IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        final Intent status = context.registerReceiver(null, filter);
        if (status == null) {
            logger.error("BatteryLevel", "Failed getting the battery status intent");
            return null;
        }
        final int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        final int max = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level < 0 || max < 0) {
            logger.error("BatteryLevel", "Failed getting the current level or max level.");
            return null;
        }

        final int percentage = (int) (((double) level / max) * 100);

        final String result;
        if (percentage >= 80) {
            result = "high";
        } else if (percentage >= 40) {
            result = "medium";
        } else if (percentage >= 20) {
            result = "low";
        } else {
            result = "very low";
        }

        return CompletableFuture.completedFuture(result);
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_battery_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return null;
    }
}
