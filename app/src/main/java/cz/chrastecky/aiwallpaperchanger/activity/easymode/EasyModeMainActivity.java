package cz.chrastecky.aiwallpaperchanger.activity.easymode;

import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityEasyModeMainBinding;
import cz.chrastecky.aiwallpaperchanger.dto.EasyModePrompt;
import cz.chrastecky.aiwallpaperchanger.easymode.EasyModePromptManager;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public class EasyModeMainActivity extends AppCompatActivity {
    private final Logger logger = new Logger(this);
    private final EasyModePromptManager promptManager = new EasyModePromptManager(this);

    private int padding;
    private int radius;
    private int itemWidth;
    private int itemHeight;
    private float fontSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupExceptionLogging();
        calculateSizes();

        ActivityEasyModeMainBinding binding = ActivityEasyModeMainBinding.inflate(getLayoutInflater());
        binding.setScrollViewPadding(itemWidth / 2 + padding);

        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        List<EasyModePrompt> prompts = promptManager.getEnrichedPrompts(promptManager.getPrompts()).join();

        for (EasyModePrompt prompt : prompts) {
            binding.promptsWrapper.addView(createPromptLayout(prompt));
        }
    }

    @NonNull
    private LinearLayout createPromptLayout(EasyModePrompt prompt) {
        LinearLayout root = new LinearLayout(this);
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rootParams.rightMargin = padding;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(rootParams);
        root.setOnClickListener(view -> {
            Toast.makeText(this, "TODO", Toast.LENGTH_LONG).show();
        });

        CardView wrapper = new CardView(this);
        wrapper.setLayoutParams(new CardView.LayoutParams(CardView.LayoutParams.WRAP_CONTENT, CardView.LayoutParams.WRAP_CONTENT));
        wrapper.setRadius(radius);
        root.addView(wrapper);

        ImageView image = new ImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams(itemWidth, itemHeight));
        image.setImageBitmap(prompt.getImage());
        wrapper.addView(image);

        TextView title = new TextView(this);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(itemWidth, itemHeight);
        titleParams.topMargin = padding / 2;
        title.setLayoutParams(titleParams);
        title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        title.setText(prompt.getName());
        title.setTextSize(fontSize);
        root.addView(title);

        return root;
    }

    private void calculateSizes() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point size = new Point();
        windowManager.getDefaultDisplay().getRealSize(size);

        final int screenWidth = size.x;

        fontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8, getResources().getDisplayMetrics());
        padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        radius = padding;
        itemWidth = (screenWidth - (padding * 3)) / 2;
        itemHeight = itemWidth * 2;
    }

    private void setupExceptionLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }

        ThreadHelper.setupGraphicalErrorHandler(logger, this);
    }
}