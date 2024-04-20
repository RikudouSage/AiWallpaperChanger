package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
    public SharedPreferences get(Context context) {
        return context.getSharedPreferences("GlobalPreferences", Context.MODE_PRIVATE);
    }
}
