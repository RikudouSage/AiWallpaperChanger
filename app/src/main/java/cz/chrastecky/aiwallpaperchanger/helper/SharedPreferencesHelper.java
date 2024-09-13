package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class SharedPreferencesHelper {
    public static final String ALLOW_LARGE_NUMERIC_VALUES = "allowLargeNumericValues";
    public static final String API_KEY = "api_key";
    public static final String STORE_WALLPAPERS_URI = "storeWallpapersUri";
    public static final String CONFIGURED_SCHEDULE_INTERVAL = "selectedInterval";
    public static final String STORED_GENERATION_PARAMETERS = "generationParameters";
    public static final String NSFW_TOGGLED = "nsfwToggled";
    public static final String WALLPAPER_LAST_CHANGED = "lastChanged";
    public static final String ADVANCED_OPTIONS_TOGGLED = "advanced";
    public static final String GENERATION_HISTORY = "history";
    public static final String DATA_SAVER_DO_NOT_ASK = "data_saver_exception_do_not_ask";
    public static final String DOZE_MODE_DO_NOT_ASK = "doze_mode_exception_do_not_ask";
    public static final String LAST_KNOWN_LOCATION = "lastKnownLocation";
    public static final String WALLPAPER_ACTION = "wallpaperAction";
    public static final String LLM_PROVIDER = "llmProvider";
    public static final String EXTRA_SLOW_WORKERS = "extraSlowWorkers";

    public SharedPreferences get(@NonNull Context context) {
        return context.getSharedPreferences("GlobalPreferences", Context.MODE_PRIVATE);
    }
}
