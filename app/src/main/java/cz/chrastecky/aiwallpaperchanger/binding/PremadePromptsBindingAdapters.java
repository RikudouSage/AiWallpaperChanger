package cz.chrastecky.aiwallpaperchanger.binding;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.databinding.BindingAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.helper.ComposableOnClickListener;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;

public class PremadePromptsBindingAdapters {
    private static final Map<Integer, ComposableOnClickListener> listenerMap = new HashMap<>();
    private static final List<String> loading = new ArrayList<>();
    private static final Map<String, List<Bitmap>> cache = new HashMap<>();

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
        final Logger logger = new Logger(context);

        loading.add(name);
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        group.addView(progressBar);

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        requestQueue.add(new JsonArrayRequest(BuildConfig.EXAMPLES_URL + "/" + name + "/index.json", response -> {
            List<String> imageNames = new ArrayList<>();
            for (int i = 0; i < response.length(); ++i) {
                try {
                    imageNames.add(response.getString(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            List<String> urls = imageNames.stream()
                    .map(image -> BuildConfig.EXAMPLES_URL + "/" + name + "/" + image)
                    .collect(Collectors.toList());
            List<Bitmap> result = new ArrayList<>();

            for (String url : urls) {
                requestQueue.add(new ImageRequest(
                        url,
                        image -> {
                            result.add(image);
                            if (result.size() == urls.size()) {
                                finalizeImageLoading(context, group, name, result, progressBar);
                            }
                        },
                        1_000_000,
                        1_000_000,
                        ImageView.ScaleType.CENTER,
                        Bitmap.Config.RGB_565,
                        error -> {
                            // todo
                            logger.error("ExampleRequestError", "Failed downloading example image " + url, error);
                        }
                ));
            }
        }, error -> {
            // todo
            group.removeView(progressBar);
        }));
    }

    private static void finalizeImageLoading(Context context, ViewGroup group, String name, List<Bitmap> images, ProgressBar progressBar) {
        group.removeView(progressBar);
    }
}
