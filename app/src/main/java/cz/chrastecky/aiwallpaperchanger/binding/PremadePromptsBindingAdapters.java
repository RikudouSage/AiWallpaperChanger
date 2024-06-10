package cz.chrastecky.aiwallpaperchanger.binding;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.databinding.BindingAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cz.chrastecky.aiwallpaperchanger.helper.ComposableOnClickListener;

public class PremadePromptsBindingAdapters {
    private static final Map<Integer, ComposableOnClickListener> listenerMap = new HashMap<>();
    private static final List<String> loading = new ArrayList<>();

    @BindingAdapter("toggleVisibility")
    public static void toggleVisibility(View source, View target) {
        setOnClickListener(source, view -> target.setVisibility(target.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    }

    @BindingAdapter("toggleButtonRotation")
    public static void toggleButtonRotation(View source, ImageButton target) {
        setOnClickListener(source, view -> target.setRotation(target.getRotation() == 180 ? 0 : 180));
    }

    @BindingAdapter({"exampleImagesTarget", "exampleImagesGroupName"})
    public static void loadExampleImages(View source, ViewGroup target, String name) {
        setOnClickListener(source, view -> {
            if (loading.contains(name)) {
                return;
            }
            loadImages(view.getContext(), target, name);
        });
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

    private static void loadImages(Context context, ViewGroup group, String name) {
        loading.add(name);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        group.addView(progressBar);
    }
}
