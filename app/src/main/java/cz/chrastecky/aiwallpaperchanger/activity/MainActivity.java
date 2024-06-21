package cz.chrastecky.aiwallpaperchanger.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.AuthFailureError;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityMainBinding;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;
import cz.chrastecky.aiwallpaperchanger.dto.Sampler;
import cz.chrastecky.aiwallpaperchanger.dto.Upscaler;
import cz.chrastecky.aiwallpaperchanger.dto.response.ActiveModel;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.exception.ContentCensoredException;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;
import cz.chrastecky.aiwallpaperchanger.helper.AlarmManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.BillingHelper;
import cz.chrastecky.aiwallpaperchanger.helper.GenerateRequestHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PermissionHelper;
import cz.chrastecky.aiwallpaperchanger.helper.PremadePromptHelper;
import cz.chrastecky.aiwallpaperchanger.helper.PromptReplacer;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ShortcutManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ValueWrapper;
import cz.chrastecky.aiwallpaperchanger.provider.AiHorde;
import cz.chrastecky.aiwallpaperchanger.provider.AiProvider;

public class MainActivity extends AppCompatActivity {
    private static final String DEFAULT_MODEL = "ICBINP - I Can't Believe It's Not Photography";
    private static final String DEFAULT_SAMPLER = Sampler.k_dpmpp_sde.name();
    private AiProvider aiProvider;
    private final Logger logger = new Logger(this);
    private ActivityMainBinding binding;

    private Map<String, Boolean> formElementsValidation = new HashMap<>();

    private List<String> selectedModels = new ArrayList<>();
    private List<String> allModels = new ArrayList<>();

    private final ActivityResultLauncher<Intent> selectModelsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, R.string.app_select_models_selecting_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                assert result.getData() != null;
                selectedModels = result.getData().getStringArrayListExtra("resultModels");
                binding.setSelectedModels(selectedModels);
            }
    );
    private final ActivityResultLauncher<Intent> selectStyleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK) {
                    return;
                }
                assert result.getData() != null;

                Intent data = result.getData();
                String styleName = data.getStringExtra("result");

                try {
                    PremadePrompt prompt = PremadePromptHelper.findByName(this, styleName);
                    if (prompt == null) {
                        logger.error("AiWallpaperChanger", "Prompt with name " + styleName + "is null");
                        Toast.makeText(this, R.string.app_premade_prompts_failed_getting, Toast.LENGTH_LONG).show();
                        return;
                    }

                    GenerateRequest request = GenerateRequestHelper.withStyle(createGenerateRequest(), prompt);
                    initializeForm(request);
                    Toast.makeText(this, R.string.app_generate_style_successfully_set, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    logger.error("AiWallpaperChanger", "Failed getting prompts", e);
                    Toast.makeText(this, R.string.app_premade_prompts_failed_getting, Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupExceptionLogging();

        this.aiProvider = new AiHorde(this);

        SharedPreferences sharedPreferences = new SharedPreferencesHelper().get(this);
        GenerateRequest request = null;
        if (sharedPreferences.contains(SharedPreferencesHelper.STORED_GENERATION_PARAMETERS)) {
            request = GenerateRequestHelper.parse(sharedPreferences.getString(SharedPreferencesHelper.STORED_GENERATION_PARAMETERS, ""));
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        binding.setSelectedModels(selectedModels);
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.getOverflowIcon().setColorFilter(getColor(R.color.md_theme_onPrimary), PorterDuff.Mode.SRC_ATOP);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.history_menu_item) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.settings_menu_item) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.premium_menu_item) {
                startActivity(new Intent(this, PremiumActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.help_menu_item) {
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.about_menu_item) {
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.next_image_menu_item) {
                startActivity(new Intent(this, TriggerNextImageActivity.class));
                return true;
            }

            return false;
        });


        binding.nsfwSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SharedPreferencesHelper.NSFW_TOGGLED, isChecked);
            editor.apply();
        });

        ConstraintLayout rootView = findViewById(R.id.rootView);
        ConstraintLayout loader = findViewById(R.id.loader);

        rootView.setVisibility(View.INVISIBLE);
        loader.setVisibility(View.VISIBLE);

        initializeValidations();
        initializeForm(request);
        this.validate();

        binding.selectPromptButton.setOnClickListener(view -> {
            selectStyleLauncher.launch(new Intent(this, PremadePromptsActivity.class));
        });

        Button previewButton = findViewById(R.id.preview_button);
        previewButton.setOnClickListener(button -> {
            if (getCurrentFocus() != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

//            File imageFile = new File(getFilesDir(), "currentImage.webp");
//            Intent intent = new Intent(this, PreviewActivity.class);
//            intent.putExtra("imagePath", imageFile.getAbsolutePath());
//            intent.putExtra("generationParameters", new Gson().toJson(createGenerateRequest()));
//            intent.putExtra("seed", "123");
//            intent.putExtra("workerId", "abcde");
//            intent.putExtra("workerName", "Fake worker");
//            startActivity(intent);

            TextView progressText = findViewById(R.id.progress_info);

            AtomicBoolean hasBeenAboveZero = new AtomicBoolean(false);

            AiHorde.OnProgress onProgress = progress -> {
                if (progress.getWaitTime() > 0) {
                    hasBeenAboveZero.set(true);
                }
                if (hasBeenAboveZero.get()) {
                    if (progress.getWaitTime() > 0) {
                        progressText.setText(getString(R.string.app_generate_estimated_time, progress.getWaitTime()));
                    } else {
                        progressText.setText(R.string.app_generate_estimated_time_too_long);
                    }
                }
                logger.debug("HordeRequestProgress", "Remaining: " + progress.getWaitTime());
            };

            AiHorde.OnResponse<GenerationDetailWithBitmap> onResponse = response -> {
                try {
                    File imageFile = new File(getFilesDir(), "currentImage.webp");
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                    imageFile.createNewFile();
                    FileOutputStream imageOutputStream = new FileOutputStream(imageFile, false);
                    response.getImage().compress(Bitmap.CompressFormat.WEBP, 100, imageOutputStream);
                    imageOutputStream.close();

                    Intent intent = new Intent(this, PreviewActivity.class);
                    intent.putExtra("imagePath", imageFile.getAbsolutePath());
                    intent.putExtra("generationParameters", new Gson().toJson(createGenerateRequest()));
                    intent.putExtra("generationParametersReplaced", new Gson().toJson(createGenerateRequest(true)));
                    intent.putExtra("seed", response.getDetail().getSeed());
                    intent.putExtra("workerId", response.getDetail().getWorkerId());
                    intent.putExtra("workerName", response.getDetail().getWorkerName());
                    startActivity(intent);
                } catch (IOException e) {
                    Toast.makeText(this, R.string.app_error_create_tmp_file, Toast.LENGTH_LONG).show();
                }

                rootView.setVisibility(View.VISIBLE);
                loader.setVisibility(View.INVISIBLE);
            };

            ValueWrapper<AiHorde.OnError> onError = new ValueWrapper<>();
            AtomicInteger censoredRetries = new AtomicInteger(3);
            onError.value = error -> {
                if (error.getCause() instanceof RetryGenerationException) {
                    GenerateRequest newRequest = createGenerateRequest(true);
                    if (newRequest == null) {
                        return;
                    }
                    aiProvider.generateImage(newRequest, onProgress, onResponse, onError.value);
                    return;
                }
                if (error.getCause() instanceof ContentCensoredException && censoredRetries.get() > 0) {
                    logger.debug("HordeError", "Request got censored, retrying");
                    censoredRetries.addAndGet(-1);

                    GenerateRequest newRequest = createGenerateRequest(true);
                    if (newRequest == null) {
                        return;
                    }
                    aiProvider.generateImage(newRequest, onProgress, onResponse, onError.value);
                    return;
                }
                if (error instanceof AuthFailureError) {
                    if (error.networkResponse.statusCode == 401) {
                        Toast.makeText(this, R.string.app_error_invalid_api_key, Toast.LENGTH_LONG).show();
                    } else if (error.networkResponse.statusCode == 403) {
                        Toast.makeText(this, R.string.app_error_forbidden_request, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.app_error_generating_failed, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.app_error_generating_failed, Toast.LENGTH_LONG).show();
                }
                rootView.setVisibility(View.VISIBLE);
                loader.setVisibility(View.INVISIBLE);
            };

            logger.debug("HordeRequest", new Gson().toJson(createGenerateRequest()));
            logger.debug("HordeRequestReplaced", new Gson().toJson(createGenerateRequest(true)));

            GenerateRequest newRequest = createGenerateRequest(true);
            if (newRequest == null) {
                return;
            }
            aiProvider.generateImage(newRequest, onProgress, onResponse, onError.value);

            rootView.setVisibility(View.INVISIBLE);
            loader.setVisibility(View.VISIBLE);
            progressText.setText(R.string.app_generate_estimated_time_pre_start);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = AlarmManagerHelper.getAlarmIntent(this, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            Button cancelButton = findViewById(R.id.cancel_schedule_button);
            cancelButton.setOnClickListener(view -> {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                cancelButton.setVisibility(View.INVISIBLE);

                SharedPreferences.Editor sharedPreferences = new SharedPreferencesHelper().get(this).edit();
                sharedPreferences.remove(SharedPreferencesHelper.CONFIGURED_SCHEDULE_INTERVAL);
                sharedPreferences.apply();

                ShortcutManagerHelper.hideShortcuts(this);

                Toast.makeText(this, R.string.app_succes_cancelled, Toast.LENGTH_LONG).show();
            });
            cancelButton.setVisibility(View.VISIBLE);
            ShortcutManagerHelper.createShortcuts(this);
        } else {
            ShortcutManagerHelper.hideShortcuts(this);
        }

        SharedPreferences sharedPreferences = new SharedPreferencesHelper().get(this);
        if (sharedPreferences.contains(SharedPreferencesHelper.WALLPAPER_LAST_CHANGED)) {
            TextView lastChanged = findViewById(R.id.last_changed);
            String lastChangedTime = sharedPreferences.getString(SharedPreferencesHelper.WALLPAPER_LAST_CHANGED, "");
            lastChanged.setText(getString(R.string.app_generate_last_generated, lastChangedTime));
            lastChanged.setVisibility(View.VISIBLE);
        }

        BillingHelper.getPurchaseStatus(this, PremiumActivity.PREMIUM_PURCHASE_NAME, status -> {
            if (!status) {
                return;
            }

            AiHorde.DEFAULT_API_KEY = BuildConfig.PREMIUM_API_KEY;
        });

        if (PermissionHelper.shouldCheckForPermissions(this)) {
            PermissionHelper.askForDataSaverException(this);
            PermissionHelper.askForDozeModeException(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu, menu);

        if (BuildConfig.BILLING_ENABLED) {
            menu.findItem(R.id.premium_menu_item).setVisible(true);
        }
        if (AlarmManagerHelper.getAlarmIntent(this, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE) != null) {
            menu.findItem(R.id.next_image_menu_item).setVisible(true);
        }

        return true;
    }

    private String getUpscaler(int[] widthAndHeight) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        double coefficient = Math.max(
                (double) metrics.widthPixels / widthAndHeight[0],
                (double) metrics.heightPixels / widthAndHeight[1]
        );

        if (coefficient > 2) {
            return Upscaler.RealESRGAN_x4plus;
        }
        if (coefficient > 1) {
            return Upscaler.RealESRGAN_x2plus;
        }

        return null;
    }

    private int[] calculateWidthAndHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int requestedWidth = metrics.widthPixels;
        int requestedHeight = metrics.heightPixels;

        double coefficientWidth = (double) requestedWidth / 1024;
        double coefficientHeight = (double) requestedHeight / 1024;
        double coefficient = coefficientWidth > coefficientHeight ? coefficientHeight / coefficientWidth : coefficientWidth / coefficientHeight;
        double width = requestedWidth > requestedHeight ? 1024 : 1024 * coefficient;
        double height = requestedHeight > requestedWidth ? 1024 : 1024 * coefficient;

        if (width % 64 != 0) {
            width += 64 - (width % 64);
        }
        if (height % 64 != 0) {
            height += 64 - (height % 64);
        }

        return new int[] {(int) width, (int) height};
    }

    private void validate() {
        Button previewButton = findViewById(R.id.preview_button);
        for (String key : formElementsValidation.keySet()) {
            if (!formElementsValidation.get(key)) {
                setButtonEnabled(previewButton, false);
                return;
            }
        }

        setButtonEnabled(previewButton, true);
    }

    private void initializeValidations() {
        this.formElementsValidation.put("prompt", false);
        this.formElementsValidation.put("width", true);
        this.formElementsValidation.put("height", true);

        TextInputEditText prompt = findViewById(R.id.prompt_field);
        TextInputEditText width = findViewById(R.id.width_field);
        TextInputEditText height = findViewById(R.id.height_field);

        prompt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                formElementsValidation.put("prompt", s.length() > 0);
                validate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        width.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int value = Integer.parseInt(s.toString());
                    formElementsValidation.put("width", value % 64 == 0);
                } catch (NumberFormatException e) {
                    formElementsValidation.put("width", false);
                }
                validate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        height.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int value = Integer.parseInt(s.toString());
                    formElementsValidation.put("height", value % 64 == 0);
                } catch (NumberFormatException e) {
                    formElementsValidation.put("height", false);
                }
                validate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void initializeForm(@Nullable GenerateRequest request) {
        ConstraintLayout rootView = findViewById(R.id.rootView);
        ConstraintLayout loader = findViewById(R.id.loader);
        SwitchCompat advancedSwitch = findViewById(R.id.advanced_switch);
        Spinner samplerField = findViewById(R.id.sampler_field);
        Slider stepsField = findViewById(R.id.steps_slider);
        TextView stepsTitle = findViewById(R.id.steps_title);
        Slider clipSkipField = findViewById(R.id.clip_skip_slider);
        TextView clipSkipTitle = findViewById(R.id.clip_skip_title);
        TextInputEditText widthField = findViewById(R.id.width_field);
        TextInputEditText heightField = findViewById(R.id.height_field);
        Spinner upscalerField = findViewById(R.id.upscaler);
        SwitchCompat nsfwField = findViewById(R.id.nsfw_switch);
        Slider cfgScaleField = findViewById(R.id.cfg_scale);
        TextView cfgScaleTitle = findViewById(R.id.cfg_scale_title);
        SwitchCompat hiresFixField = findViewById(R.id.hires_fix_switch);
        SwitchCompat multipleModelsSwitch = findViewById(R.id.multiple_models_switch);
        Spinner modelField = findViewById(R.id.model_field);
        Button modelSelectButton = findViewById(R.id.model_select_button);
        TextView modelSelectedList = findViewById(R.id.model_selected_list);

        SharedPreferences preferences = new SharedPreferencesHelper().get(this);
        int[] widthHeight = calculateWidthAndHeight();

        List<String> samplers = Sampler.getEntries().stream().map(Enum::name).collect(Collectors.toList());
        samplerField.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                samplers
        ));
        samplerField.setSelection(samplers.indexOf(DEFAULT_SAMPLER));

        List<String> upscalers = Arrays.stream(Upscaler.class.getFields())
                .map(field -> {
                    try {
                        return field.get(null).toString();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        upscalers.add(0, getString(R.string.app_select_empty_option));
        upscalerField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, upscalers));
        upscalerField.setSelection(upscalers.indexOf(getUpscaler(widthHeight)));

        advancedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(SharedPreferencesHelper.ADVANCED_OPTIONS_TOGGLED, isChecked);
            editor.apply();

            ConstraintLayout advancedWrapper = findViewById(R.id.advanced_settings_wrapper);
            advancedWrapper.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        advancedSwitch.setChecked(preferences.getBoolean(SharedPreferencesHelper.ADVANCED_OPTIONS_TOGGLED, false));

        stepsField.addOnChangeListener((slider, value, fromUser) -> stepsTitle.setText(getString(R.string.app_generate_steps, (int) value)));
        stepsTitle.setText(getString(R.string.app_generate_steps, 25));

        clipSkipField.addOnChangeListener((slider, value, fromUser) -> clipSkipTitle.setText(getString(R.string.app_generate_clip_skip, (int) value)));
        clipSkipTitle.setText(getString(R.string.app_generate_clip_skip, 1));

        widthField.setText(String.valueOf(widthHeight[0]));
        heightField.setText(String.valueOf(widthHeight[1]));

        cfgScaleField.addOnChangeListener((slider, value, fromUser) -> cfgScaleTitle.setText(getString(R.string.app_generate_cfg_scale, value)));
        cfgScaleTitle.setText(getString(R.string.app_generate_cfg_scale, 7f));

        if (BuildConfig.NSFW_ENABLED) {
            nsfwField.setVisibility(View.VISIBLE);
            nsfwField.setChecked(preferences.getBoolean(SharedPreferencesHelper.NSFW_TOGGLED, false));
        }

        multipleModelsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonView.setText(isChecked ? R.string.app_generate_models_multiple : R.string.app_generate_models_single);
            modelField.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            modelSelectButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            modelSelectedList.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (!isChecked && !selectedModels.isEmpty()) {
                modelField.setSelection(allModels.indexOf(selectedModels.get(0)));
            }
        });

        modelSelectButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectModelsActivity.class);
            intent.putStringArrayListExtra("selectedModels", new ArrayList<>(selectedModels));
            intent.putStringArrayListExtra("allModels", new ArrayList<>(allModels));
            selectModelsLauncher.launch(intent);
        });

        if (request != null) {
            TextInputEditText prompt = findViewById(R.id.prompt_field);
            TextInputEditText negativePrompt = findViewById(R.id.negative_prompt_field);

            prompt.setText(request.getPrompt());
            negativePrompt.setText(request.getNegativePrompt());
            samplerField.setSelection(samplers.indexOf(request.getSampler().name()));
            stepsField.setValue(request.getSteps());
            clipSkipField.setValue(request.getClipSkip());
            widthField.setText(String.valueOf(request.getWidth()));
            heightField.setText(String.valueOf(request.getHeight()));
            cfgScaleField.setValue((float) request.getCfgScale());
            hiresFixField.setChecked(request.getHiresFix());
            multipleModelsSwitch.setChecked(request.getModels().size() > 1);
        }

        aiProvider.getModels(response -> {
            response.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(), b.getName()));
            List<String> models = response.stream().map(ActiveModel::getName).collect(Collectors.toList());
            this.allModels = models;
            modelField.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, models));
            modelField.setSelection(models.indexOf(DEFAULT_MODEL));

            if (request != null && !multipleModelsSwitch.isChecked()) {
                modelField.setSelection(models.indexOf(request.getModels().get(0)));
            }
            if (request != null) {
                this.selectedModels = request.getModels();
                binding.setSelectedModels(this.selectedModels);
            }

            modelField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedModels = Collections.singletonList(models.get(position));
                    binding.setSelectedModels(selectedModels);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedModels = new ArrayList<>();
                    binding.setSelectedModels(selectedModels);
                }
            });

            loader.setVisibility(View.INVISIBLE);
            rootView.setVisibility(View.VISIBLE);
        }, error -> {
            logger.error("AiHorde", "Fetching list of models failed", error);
            Toast.makeText(this, R.string.app_error_fetching_models_failed, Toast.LENGTH_LONG).show();
        });
    }

    private GenerateRequest createGenerateRequest() {
        return createGenerateRequest(false);
    }

    @Nullable
    private GenerateRequest  createGenerateRequest(boolean replacePrompt) {
        TextInputEditText prompt = findViewById(R.id.prompt_field);
        TextInputEditText negativePrompt = findViewById(R.id.negative_prompt_field);
        Spinner sampler = findViewById(R.id.sampler_field);
        Slider steps = findViewById(R.id.steps_slider);
        Slider clipSkip = findViewById(R.id.clip_skip_slider);
        TextInputEditText width = findViewById(R.id.width_field);
        TextInputEditText height = findViewById(R.id.height_field);
        Spinner upscaler = findViewById(R.id.upscaler);
        SwitchCompat nsfw = findViewById(R.id.nsfw_switch);
        Slider cfgScale = findViewById(R.id.cfg_scale);
        SwitchCompat hiresFix = findViewById(R.id.hires_fix_switch);

        boolean advanced = ((SwitchCompat) findViewById(R.id.advanced_switch)).isChecked();

        int[] widthAndHeight = this.calculateWidthAndHeight();
        String defaultUpscaler = this.getUpscaler(widthAndHeight);

        String selectedUpscaler = (String) upscaler.getSelectedItem();
        String noneOption = getString(R.string.app_select_empty_option);

        String promptText = Objects.requireNonNull(prompt.getText()).toString();

        if (replacePrompt) {
            promptText = PromptReplacer.replacePrompt(this, promptText, true);
            if (promptText == null) {
                return null;
            }
        }

        return new GenerateRequest(
                promptText,
                negativePrompt.getText().length() > 0 ? negativePrompt.getText().toString() : null,
                selectedModels,
                advanced ? Sampler.valueOf((String) sampler.getSelectedItem()) : Sampler.k_dpmpp_sde,
                advanced ? (int) steps.getValue() : 25,
                advanced ? (int) clipSkip.getValue() : 1,
                advanced ? Integer.parseInt(width.getText().toString()) : widthAndHeight[0],
                advanced ? Integer.parseInt(height.getText().toString()) : widthAndHeight[1],
                null,
                advanced ? (selectedUpscaler.equals(noneOption) ? null : selectedUpscaler) : defaultUpscaler,
                advanced ? cfgScale.getValue() : 7,
                BuildConfig.NSFW_ENABLED && nsfw.isChecked(),
                true,
                advanced && hiresFix.isChecked()
        );
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setBackgroundResource(enabled ? R.color.md_theme_primary : android.R.color.darker_gray);
        button.setTextColor(getResources().getColor(enabled ? R.color.md_theme_onPrimary : android.R.color.black, null));
    }

    private void setupExceptionLogging() {
        if (BuildConfig.DEBUG) {
            return;
        }
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            logger.error("UncaughtException", "There was an uncaught exception", exception);

            Intent intent = new Intent(this, UncaughtErrorActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }
}