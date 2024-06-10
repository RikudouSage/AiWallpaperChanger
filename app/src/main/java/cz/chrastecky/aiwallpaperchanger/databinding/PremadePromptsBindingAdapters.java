package cz.chrastecky.aiwallpaperchanger.databinding;

import android.view.View;
import android.widget.ImageButton;

import androidx.databinding.BindingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cz.chrastecky.aiwallpaperchanger.helper.ComposableOnClickListener;

public class PremadePromptsBindingAdapters {
    private static final Map<Integer, ComposableOnClickListener> listenerMap = new HashMap<>();

    @BindingAdapter("toggleVisibility")
    public static void toggleVisibility(View source, View target) {
        setOnClickListener(source, view -> target.setVisibility(target.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    }

    @BindingAdapter("toggleButtonRotation")
    public static void toggleButtonRotation(View source, ImageButton target) {
        setOnClickListener(source, view -> target.setRotation(target.getRotation() == 180 ? 0 : 180));
    }

    private static void setOnClickListener(View target, View.OnClickListener listener) {
        int id = System.identityHashCode(target);
        if (!listenerMap.containsKey(id)) {
            listenerMap.put(id, new ComposableOnClickListener());
            target.setOnClickListener(listenerMap.get(id));
        }
        ComposableOnClickListener wrapper = Objects.requireNonNull(listenerMap.get(id));
        wrapper.addListener(listener);
    }
}
