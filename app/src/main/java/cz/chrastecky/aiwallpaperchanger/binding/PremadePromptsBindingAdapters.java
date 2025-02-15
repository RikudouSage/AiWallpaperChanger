package cz.chrastecky.aiwallpaperchanger.binding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;
import cz.chrastecky.aiwallpaperchanger.exception.PromptNotFoundException;
import cz.chrastecky.aiwallpaperchanger.helper.ComposableOnClickListener;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PremadePromptHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

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

    @BindingAdapter({"exampleImagesTarget", "exampleImagesGroupName", "itemIgnoreImages"})
    public static void loadExampleImages(View source, ViewGroup target, String name, Boolean ignoreImages) {
        setOnClickListener(source, view -> {
            target.removeAllViews();
            if (ignoreImages) {
                ThreadHelper.runInThread(
                        () -> finalizeImageLoading((Activity) source.getContext(), target, new ArrayList<>(), name, null),
                        source.getContext()
                );
                return;
            }

            if (cache.containsKey(name)) {
                finalizeImageLoading((Activity) view.getContext(), target, cache.get(name), name, null);
                return;
            }
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
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        group.addView(progressBar);

        RequestQueue requestQueue = Volley.newRequestQueue(context);

        final String indexUrl = BuildConfig.EXAMPLES_URL + "/" + name + "/index.json";
        requestQueue.add(new JsonArrayRequest(indexUrl, response -> {
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
                                finalizeImageLoading((Activity) context, group, result, name, progressBar);
                            }
                        },
                        1_000_000,
                        1_000_000,
                        ImageView.ScaleType.CENTER,
                        Bitmap.Config.RGB_565,
                        error -> {
                            Toast.makeText(context, R.string.app_premade_styles_downloading_failed, Toast.LENGTH_LONG).show();
                            logger.error("ExampleRequestError", "Failed downloading example image " + url, error);
                        }
                ));
            }
        }, error -> {
            Toast.makeText(context, R.string.app_premade_styles_downloading_failed, Toast.LENGTH_LONG).show();
            logger.error("ExampleRequestError", "Failed downloading example images json from " + indexUrl, error);
            group.removeView(progressBar);
        }));
    }

    private static void finalizeImageLoading(Activity context, ViewGroup group, List<Bitmap> images, String name, @Nullable ProgressBar progressBar) {
        final Logger logger = new Logger(context);

        if (progressBar != null) {
            context.runOnUiThread(() -> group.removeView(progressBar));
        }

        PremadePrompt prompt;
        try {
            prompt = PremadePromptHelper.findByName(context, name);
            if (prompt == null) {
                prompt = PremadePromptHelper.findByNameFromDb(context, name);
            }
            if (prompt == null) {
                throw new PromptNotFoundException();
            }
        } catch (IOException e) {
            logger.error("PremadePrompts", "Failed getting prompts", e);
            context.runOnUiThread(() -> Toast.makeText(context, R.string.app_error_unknown_style, Toast.LENGTH_LONG).show());
            return;
        } catch (PromptNotFoundException e) {
            logger.error("PremadePrompts", "Failed getting prompt " + name, e);
            context.runOnUiThread(() -> Toast.makeText(context, R.string.app_error_unknown_style, Toast.LENGTH_LONG).show());
            return;
        }

        final PremadePrompt finalizedPrompt = prompt;

        context.runOnUiThread(() -> {
            HorizontalScrollView scrollView = new HorizontalScrollView(context);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            group.addView(scrollView);

            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            scrollView.addView(wrapper);

            int dp8 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());

            if (!images.isEmpty()) {
                for (Bitmap image : images) {
                    ImageView imageView = new ImageView(context);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMarginEnd(dp8);

                    imageView.setImageBitmap(image);
                    imageView.setLayoutParams(params);
                    wrapper.addView(imageView);
                }
            } else {
                TextView promptView = new TextView(context);
                promptView.setText(Html.fromHtml(
                        "<b>" + context.getString(R.string.app_generate_prompt) + "</b>: " + finalizedPrompt.getPrompt(),
                        Html.FROM_HTML_MODE_COMPACT
                ));
                LinearLayout.LayoutParams promptViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                promptViewParams.setMargins(0, 0, 0, dp8);
                promptViewParams.setMarginStart(dp8);
                promptView.setLayoutParams(promptViewParams);
                promptView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                wrapper.addView(promptView);
            }

            Button button = new Button(context);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.setMargins(0, dp8, 0, 0);
            button.setLayoutParams(buttonParams);
            button.setText(R.string.app_premade_prompts_use_style_button);
            group.addView(button);
            button.setOnClickListener(view -> {
                if (!(view.getContext() instanceof Activity)) {
                    logger.error("PremadePrompts", "Context is not an activity");
                    Toast.makeText(view.getContext(), R.string.app_premade_prompts_error_setting_prompt, Toast.LENGTH_LONG).show();
                    return;
                }

                Intent result = new Intent();
                result.putExtra("result", name);

                Activity activity = (Activity) view.getContext();
                activity.setResult(Activity.RESULT_OK, result);
                activity.finish();
            });

            if (finalizedPrompt.getDescription() != null) {
                TextView description = new TextView(context);
                description.setText(Html.fromHtml(context.getString(R.string.app_premade_prompt_description, finalizedPrompt.getDescription()), Html.FROM_HTML_MODE_COMPACT));
                LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                descriptionParams.setMargins(0, 0, 0, dp8);
                descriptionParams.setMarginStart(dp8);
                description.setLayoutParams(descriptionParams);
                description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                group.addView(description);
            }

            if (finalizedPrompt.getAuthor() != null) {
                TextView author = new TextView(context);
                author.setText(Html.fromHtml(context.getString(R.string.app_premade_prompt_author, finalizedPrompt.getAuthor()), Html.FROM_HTML_MODE_COMPACT));
                LinearLayout.LayoutParams authorParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                authorParams.setMarginStart(dp8);
                author.setLayoutParams(authorParams);
                group.addView(author);
            }

            cache.put(name, images);
            loading.remove(name);
        });
    }
}
