package cz.chrastecky.aiwallpaperchanger.helper;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ComposableOnClickListener implements View.OnClickListener {
    private final List<View.OnClickListener> listeners = new ArrayList<>();

    @Override
    public void onClick(View v) {
        for (View.OnClickListener listener : listeners) {
            listener.onClick(v);
        }
    }

    public void addListener(View.OnClickListener listener) {
        listeners.add(listener);
    }
}
