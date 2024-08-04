package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;

public class ApiKeyHelper {
    private static String defaultApiKey = BuildConfig.API_KEY;

    public static String getApiKey(@NonNull final Context context) {
        return SharedPreferencesHelper.get(context).getString(SharedPreferencesHelper.API_KEY, defaultApiKey);
    }

    public static String getDefaultApiKey() {
        return defaultApiKey;
    }

    public static void setDefaultApiKey(@NonNull final String apiKey) {
        defaultApiKey = apiKey;
    }
}
