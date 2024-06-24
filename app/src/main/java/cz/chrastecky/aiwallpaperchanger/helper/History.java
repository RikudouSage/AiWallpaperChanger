package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;

public class History {
    private final int MAX_ITEMS = 96;
    private final Context context;

    public History(Context context) {
        this.context = context;
    }

    public void addItem(StoredRequest item) {
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
        Set<String> history = new HashSet<>(preferences.getStringSet(SharedPreferencesHelper.GENERATION_HISTORY, new HashSet<>()));
        history.add(new Gson().toJson(item));
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(SharedPreferencesHelper.GENERATION_HISTORY, history);
        editor.apply();
    }

    public List<StoredRequest> getHistory() {
        return getHistory(true);
    }
    public List<StoredRequest> getHistory(boolean sorted) {
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
        Set<String> raw = preferences.getStringSet(SharedPreferencesHelper.GENERATION_HISTORY, new HashSet<>());

        List<StoredRequest> result = new ArrayList<>();
        for (String item : raw) {
            result.add(new Gson().fromJson(item, StoredRequest.class));
        }

        if (sorted) {
            result.sort((a, b) -> {
                if (a.getCreated().equals(b.getCreated())) {
                    return 0;
                }

                return a.getCreated().after(b.getCreated()) ? -1 : 1;
            });


            if (result.size() > MAX_ITEMS + 14) { // add some random buffer
                Set<String> capped = result.subList(0, MAX_ITEMS).stream().map(
                        storedRequest -> new Gson().toJson(storedRequest)
                ).collect(Collectors.toSet());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putStringSet(SharedPreferencesHelper.GENERATION_HISTORY, capped);
                editor.apply();
            }
        }

        return result;
    }

    @Nullable
    public StoredRequest getItem(UUID id) {
        List<StoredRequest> result = getHistory().stream().filter(request -> request.getId().equals(id)).collect(Collectors.toList());
        if (result.isEmpty()) {
            return null;
        }

        return result.get(0);
    }
    @Nullable
    public StoredRequest getItem(String id) {
        return getItem(UUID.fromString(id));
    }
}
