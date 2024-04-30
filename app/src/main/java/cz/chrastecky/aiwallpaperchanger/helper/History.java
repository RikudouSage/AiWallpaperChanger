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
    private final Context context;

    public History(Context context) {
        this.context = context;
    }

    public void addItem(StoredRequest item) {
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
        Set<String> history = new HashSet<>(preferences.getStringSet("history", new HashSet<>()));
        history.add(new Gson().toJson(item));
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet("history", history);
        editor.apply();
    }

    public List<StoredRequest> getHistory() {
        return getHistory(true);
    }
    public List<StoredRequest> getHistory(boolean sorted) {
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
        Set<String> raw = preferences.getStringSet("history", new HashSet<>());

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