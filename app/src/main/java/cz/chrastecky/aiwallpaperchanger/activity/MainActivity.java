package cz.chrastecky.aiwallpaperchanger.activity;

import static android.widget.Toast.LENGTH_LONG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.PromptParameterProviders;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.data.AppDatabase;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;
import cz.chrastecky.aiwallpaperchanger.data.entity.SavedPrompt;
import cz.chrastecky.aiwallpaperchanger.data.relation.CustomParameterWithValues;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityMainBinding;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePrompt;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePromptCustomParameter;
import cz.chrastecky.aiwallpaperchanger.dto.PremadePromptCustomParameterCondition;
import cz.chrastecky.aiwallpaperchanger.dto.Sampler;
import cz.chrastecky.aiwallpaperchanger.dto.Upscaler;
import cz.chrastecky.aiwallpaperchanger.dto.response.ActiveModel;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.exception.ContentCensoredException;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;
import cz.chrastecky.aiwallpaperchanger.helper.AlarmManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ApiKeyHelper;
import cz.chrastecky.aiwallpaperchanger.helper.BillingHelper;
import cz.chrastecky.aiwallpaperchanger.helper.CancellationToken;
import cz.chrastecky.aiwallpaperchanger.helper.DatabaseHelper;
import cz.chrastecky.aiwallpaperchanger.helper.GenerateRequestHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PermissionHelper;
import cz.chrastecky.aiwallpaperchanger.helper.PremadePromptHelper;
import cz.chrastecky.aiwallpaperchanger.helper.PromptReplacer;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ShortcutManagerHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ValueWrapper;
import cz.chrastecky.aiwallpaperchanger.helper.WallpaperFileHelper;
import cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider.PromptParameterProvider;
import cz.chrastecky.aiwallpaperchanger.provider.AiHorde;
import cz.chrastecky.aiwallpaperchanger.provider.AiImageProvider;

public class MainActivity extends AppCompatActivity {
    private interface OnGenerateRequestCreated {
        void onCreated(@NonNull GenerateRequest request);
    }
    private interface OnGenerateRequestFailed {
        void onFailed();
    }

    private static final String DEFAULT_MODEL = "ICBINP - I Can't Believe It's Not Photography";
    private static final String DEFAULT_SAMPLER = Sampler.k_dpmpp_sde.name();
    private AiImageProvider aiImageProvider;
    private final Logger logger = new Logger(this);
    private ActivityMainBinding binding;
    private CancellationToken cancellationToken = new CancellationToken();
    private String currentStyleName = null;

    private final Map<String, Boolean> formElementsValidation = new HashMap<>();

    private List<String> selectedModels = new ArrayList<>();
    private List<String> allModels = new ArrayList<>();
    private final List<String> filenamesToDelete = new ArrayList<>();

    private final PromptParameterProviders parameterProviders = new PromptParameterProviders();

    private final ActivityResultLauncher<Intent> selectModelsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK) {
                    Toast.makeText(this, R.string.app_select_models_selecting_failed, LENGTH_LONG).show();
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
                currentStyleName = styleName;

                ThreadHelper.runInThread(() -> {
                    try {
                        PremadePrompt prompt = PremadePromptHelper.findByName(this, styleName);
                        if (prompt == null) {
                            prompt = PremadePromptHelper.findByNameFromDb(this, styleName);
                        }
                        if (prompt == null) {
                            logger.error("AiWallpaperChanger", "Prompt with name " + styleName + "is null");
                            runOnUiThread(() -> Toast.makeText(this, R.string.app_premade_prompts_failed_getting, LENGTH_LONG).show());
                            return;
                        }

                        final PremadePrompt finalPrompt = prompt;
                        runOnUiThread(() -> {
                            createGenerateRequest(request -> {
                                request = GenerateRequestHelper.withStyle(request, finalPrompt);
                                initializeForm(request);
                                Toast.makeText(this, R.string.app_generate_style_successfully_set, LENGTH_LONG).show();
                            });
                            createCustomParameters(finalPrompt);
                        });
                    } catch (IOException e) {
                        logger.error("AiWallpaperChanger", "Failed getting prompts", e);
                        runOnUiThread(() -> Toast.makeText(this, R.string.app_premade_prompts_failed_getting, LENGTH_LONG).show());
                    }
                }, this);
            }
    );

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            isGranted -> {
                if (isGranted.containsValue(false)) {
                    Toast.makeText(this, R.string.app_error_missing_permissions, LENGTH_LONG).show();
                    return;
                }

                Button preview = findViewById(R.id.preview_button);
                preview.callOnClick();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupExceptionLogging();

        this.aiImageProvider = new AiHorde(this);

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
            if (item.getItemId() == R.id.share_current_menu_item) {
                try {
                    final File tempFile = WallpaperFileHelper.save(
                            this,
                            WallpaperFileHelper.getBitmap(this),
                            "AI_Wallpaper_Changer_" + new Date().getTime() / 1_000 + ".webp"
                    );
                    filenamesToDelete.add(tempFile.getName());
                    Intent intent = WallpaperFileHelper.getShareIntent(this, tempFile);
                    startActivity(intent);
                } catch (IOException e) {
                    Toast.makeText(this, R.string.app_error_create_tmp_file, LENGTH_LONG).show();
                    return true;
                }

                return true;
            }

            return false;
        });


        binding.nsfwSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SharedPreferencesHelper.NSFW_TOGGLED, isChecked);
            editor.apply();
        });
        binding.censorNsfwSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(SharedPreferencesHelper.CENSOR_NSFW, isChecked);
            editor.apply();
        });

        binding.joinHordeText.setMovementMethod(LinkMovementMethod.getInstance());

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

        binding.cancelPreviewButton.setOnClickListener(button -> {
            cancellationToken.cancel();
            runOnUiThread(() -> {
                rootView.setVisibility(View.VISIBLE);
                loader.setVisibility(View.INVISIBLE);
            });
            cancellationToken = new CancellationToken();
        });

        binding.savePromptButton.setOnClickListener(button -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_prompt, null);
            EditText nameInputField = dialogView.findViewById(R.id.name_input);
            if (currentStyleName != null) {
                nameInputField.setText(currentStyleName);
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.app_generate_save_prompt)
                    .setView(dialogView)
                    .setCancelable(true)
                    .setPositiveButton(R.string.app_save, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
            ;

            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                final String name = nameInputField.getText().toString().trim();
                if (name.isEmpty()) {
                    nameInputField.setError(getString(R.string.app_error_field_empty));
                    return;
                }

                createGenerateRequest(createdRequest -> {
                    final SavedPrompt savedPrompt = new SavedPrompt(
                        name,
                        createdRequest.getPrompt(),
                        createdRequest.getModels()
                    );
                    if (createdRequest.getNegativePrompt() != null) {
                        savedPrompt.negativePrompt = createdRequest.getNegativePrompt();
                    }

                    if (binding.advancedSwitch.isChecked()) {
                        savedPrompt.sampler = createdRequest.getSampler();
                        savedPrompt.karras = createdRequest.getKarras();
                        savedPrompt.steps = createdRequest.getSteps();
                        savedPrompt.clipSkip = createdRequest.getClipSkip();
                        savedPrompt.width = createdRequest.getWidth();
                        savedPrompt.height = createdRequest.getHeight();
                        savedPrompt.upscaler = createdRequest.getUpscaler();
                        savedPrompt.cfgScale = createdRequest.getCfgScale();
                        savedPrompt.hiresFix = createdRequest.getHiresFix();
                    }

                    ThreadHelper.runInThread(() -> {
                        AppDatabase database = DatabaseHelper.getDatabase(this);
                        database.savedPrompts().createOrUpdate(savedPrompt);
                    }, this);

                    Toast.makeText(this, R.string.app_save_prompt_success, LENGTH_LONG).show();
                    dialog.dismiss();
                }, null, false);
            });
        });

        Button previewButton = findViewById(R.id.preview_button);
        previewButton.setOnClickListener(button -> {
            if (getCurrentFocus() != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            final String prompt = binding.promptField.getText() != null ? binding.promptField.getText().toString() : "";

            Map<String, List<String>> neededPermissions = new HashMap<>();
            for (PromptParameterProvider provider : parameterProviders.getProviders()) {
                for (String parameterName : provider.getParametersInText(this, prompt).join()) {
                    final List<String> requiredPermissions = provider.getRequiredPermissions(this, getGrantedPermissions(), parameterName);
                    if (requiredPermissions == null || requiredPermissions.isEmpty()) {
                        continue;
                    }

                    final List<String> granted = new ArrayList<>();
                    final List<String> ungranted = new ArrayList<>();
                    for (String permission : requiredPermissions) {
                        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                            granted.add(permission);
                        } else {
                            ungranted.add(permission);
                        }
                    }

                    if (provider.permissionsSatisfied(this, granted, parameterName)) {
                        continue;
                    }

                    neededPermissions.put(parameterName, ungranted);
                }
            }

            if (!neededPermissions.isEmpty()) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle(R.string.app_generate_prompt_permissions_title);

                StringBuilder permissions = new StringBuilder();
                PackageManager packageManager = getPackageManager();
                for (String parameter : neededPermissions.keySet()) {
                    permissions.append("- ${").append(parameter).append("}: ");
                    for (String permissionName : neededPermissions.get(parameter)) {
                        try {
                            PermissionInfo info = packageManager.getPermissionInfo(permissionName, PackageManager.GET_META_DATA);
                            permissions.append(info.loadLabel(packageManager)).append(", ");
                        } catch (PackageManager.NameNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    permissions = new StringBuilder(permissions.substring(0, permissions.length() - 2) + "\n\n");
                }

                dialogBuilder.setMessage(getString(R.string.app_generate_prompt_permissions, permissions.toString()));
                dialogBuilder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    requestPermissionLauncher.launch(neededPermissions.values().stream().flatMap(List::stream).toArray(String[]::new));
                });
                dialogBuilder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    // do nothing
                });
                dialogBuilder.create().show();
                return;
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
            AtomicReference<GenerateRequest> cachedRawRequest = new AtomicReference<>(null);
            AtomicReference<GenerateRequest> cachedReplacedRequest = new AtomicReference<>(null);

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
                    final File imageFile = WallpaperFileHelper.save(this, response.getImage(), "temp.webp");
                    if (!imageFile.exists()) {
                        logger.error("Main", "The image does not exist even after saving it");
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.app_error_create_tmp_file, LENGTH_LONG).show();
                            rootView.setVisibility(View.VISIBLE);
                            loader.setVisibility(View.INVISIBLE);
                        });
                        return;
                    }

                    Intent intent = new Intent(this, PreviewActivity.class);
                    intent.putExtra("imagePath", imageFile.getName());
                    intent.putExtra("generationParameters", new Gson().toJson(cachedRawRequest.get()));
                    intent.putExtra("generationParametersReplaced", new Gson().toJson(cachedReplacedRequest.get()));
                    intent.putExtra("seed", response.getDetail().getSeed());
                    intent.putExtra("workerId", response.getDetail().getWorkerId());
                    intent.putExtra("workerName", response.getDetail().getWorkerName());
                    intent.putExtra("model", response.getDetail().getModel());
                    startActivity(intent);
                } catch (IOException e) {
                    Toast.makeText(this, R.string.app_error_create_tmp_file, LENGTH_LONG).show();
                }

                runOnUiThread(() -> {
                    rootView.setVisibility(View.VISIBLE);
                    loader.setVisibility(View.INVISIBLE);
                });
            };

            ValueWrapper<AiHorde.OnError> onError = new ValueWrapper<>();
            AtomicInteger censoredRetries = new AtomicInteger(3);
            onError.value = error -> {
                if (error.getCause() instanceof RetryGenerationException) {
                    aiImageProvider.generateImage(cachedReplacedRequest.get(), onProgress, onResponse, onError.value, cancellationToken);
                    return;
                }
                if (error.getCause() instanceof ContentCensoredException && censoredRetries.get() > 0) {
                    logger.debug("HordeError", "Request got censored, retrying");
                    censoredRetries.addAndGet(-1);
                    aiImageProvider.generateImage(cachedReplacedRequest.get(), onProgress, onResponse, onError.value, cancellationToken);
                    return;
                }
                if (error instanceof AuthFailureError) {
                    if (error.networkResponse.statusCode == 401) {
                        Toast.makeText(this, R.string.app_error_invalid_api_key, LENGTH_LONG).show();
                    } else if (error.networkResponse.statusCode == 403) {
//                        String errorText = new String(error.networkResponse.data, StandardCharsets.UTF_8);
//                        logger.debug("Debug", errorText);
                        Toast.makeText(this, R.string.app_error_forbidden_request, LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.app_error_generating_failed, LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, R.string.app_error_generating_failed, LENGTH_LONG).show();
                }

                runOnUiThread(() -> {
                    rootView.setVisibility(View.VISIBLE);
                    loader.setVisibility(View.INVISIBLE);
//                    Toast.makeText(this, R.string.app_error_parameter_replacing_failed, Toast.LENGTH_LONG).show();
                });
            };

            rootView.setVisibility(View.INVISIBLE);
            loader.setVisibility(View.VISIBLE);
            progressText.setText(R.string.app_generate_estimated_time_pre_start);

            createGenerateRequest(rawRequest -> {
                logger.debug("HordeRequest", new Gson().toJson(rawRequest));
                runOnUiThread(() -> progressText.setText(R.string.app_generate_estimated_time_pre_start_parameters));
                cachedRawRequest.set(rawRequest);
                createGenerateRequest(newRequest -> {
                    logger.debug("HordeRequestReplaced", new Gson().toJson(newRequest));
                    runOnUiThread(() -> progressText.setText(R.string.app_generate_estimated_time_pre_start));
                    cachedReplacedRequest.set(newRequest);
                    aiImageProvider.generateImage(newRequest, onProgress, onResponse, onError.value, cancellationToken);
                }, () -> {
                    Toast.makeText(this, R.string.app_error_parameter_replacing_failed, LENGTH_LONG).show();
                    runOnUiThread(() -> {
                        rootView.setVisibility(View.VISIBLE);
                        loader.setVisibility(View.INVISIBLE);
                    });
                });
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        for (final String filename : filenamesToDelete) {
            if (filename == null) {
                continue;
            }

            File file = WallpaperFileHelper.getFile(this, filename);
            if (file == null) {
                continue;
            }

            file.delete();
        }
    }

    @NonNull
    private List<String> getGrantedPermissions() {
        try {
            final List<String> grantedPermissions = new ArrayList<>();
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_PERMISSIONS);
            final String[] requestedPermissions = packageInfo.requestedPermissions;
            final int[] grantResults = packageInfo.requestedPermissionsFlags;

            if (requestedPermissions != null) {
                for (int i = 0; i < requestedPermissions.length; i++) {
                    if ((grantResults[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        grantedPermissions.add(requestedPermissions[i]);
                    }
                }
            }

            return grantedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            return new ArrayList<>();
        }
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
                cancelButton.setVisibility(View.GONE);

                SharedPreferences.Editor sharedPreferences = new SharedPreferencesHelper().get(this).edit();
                sharedPreferences.remove(SharedPreferencesHelper.CONFIGURED_SCHEDULE_INTERVAL);
                sharedPreferences.apply();

                ShortcutManagerHelper.hideShortcuts(this);

                Toast.makeText(this, R.string.app_succes_cancelled, LENGTH_LONG).show();
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
                if (sharedPreferences.getString(SharedPreferencesHelper.API_KEY, "").trim().isEmpty()) {
                    binding.joinHordeText.setVisibility(View.VISIBLE);
                } else {
                    binding.joinHordeText.setVisibility(View.GONE);
                }
                return;
            }

            binding.joinHordeText.setVisibility(View.GONE);
            ApiKeyHelper.setDefaultApiKey(BuildConfig.PREMIUM_API_KEY);
        });

        if (PermissionHelper.shouldCheckForPermissions(this)) {
            PermissionHelper.askForDataSaverException(this);
            PermissionHelper.askForDozeModeException(this);
        }

        binding.setMaxCfgScale(sharedPreferences.getBoolean(SharedPreferencesHelper.ALLOW_LARGE_NUMERIC_VALUES, false) ? 100 : 20);
        binding.setMaxSteps(sharedPreferences.getBoolean(SharedPreferencesHelper.ALLOW_LARGE_NUMERIC_VALUES, false) ? 500 : 60);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);

        if (BuildConfig.BILLING_ENABLED) {
            menu.findItem(R.id.premium_menu_item).setVisible(true);
        }
        if (AlarmManagerHelper.getAlarmIntent(this, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE) != null) {
            menu.findItem(R.id.next_image_menu_item).setVisible(true);
        }
        if (WallpaperFileHelper.getFile(this) != null) {
            menu.findItem(R.id.share_current_menu_item).setVisible(true);
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
        Button saveButton = findViewById(R.id.save_prompt_button);

        for (String key : formElementsValidation.keySet()) {
            if (!formElementsValidation.get(key)) {
                setButtonEnabled(previewButton, false);
                setButtonEnabled(saveButton, false);
                return;
            }
        }

        setButtonEnabled(previewButton, true);
        setButtonEnabled(saveButton, true, R.color.md_theme_secondary, R.color.md_theme_onSecondary);
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
        SwitchCompat censorNsfwField = findViewById(R.id.censor_nsfw_switch);
        Slider cfgScaleField = findViewById(R.id.cfg_scale);
        TextView cfgScaleTitle = findViewById(R.id.cfg_scale_title);
        SwitchCompat hiresFixField = findViewById(R.id.hires_fix_switch);
        SwitchCompat multipleModelsSwitch = findViewById(R.id.multiple_models_switch);
        Spinner modelField = findViewById(R.id.model_field);
        Button modelSelectButton = findViewById(R.id.model_select_button);
        TextView modelSelectedList = findViewById(R.id.model_selected_list);
        SwitchCompat karrasField = findViewById(R.id.karras_switch);

        Button cancelPreviewButton = findViewById(R.id.cancel_preview_button);

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
        stepsTitle.setText(getString(R.string.app_generate_steps, 20));

        clipSkipField.addOnChangeListener((slider, value, fromUser) -> clipSkipTitle.setText(getString(R.string.app_generate_clip_skip, (int) value)));
        clipSkipTitle.setText(getString(R.string.app_generate_clip_skip, 1));

        widthField.setText(String.valueOf(widthHeight[0]));
        heightField.setText(String.valueOf(widthHeight[1]));

        cfgScaleField.addOnChangeListener((slider, value, fromUser) -> cfgScaleTitle.setText(getString(R.string.app_generate_cfg_scale, value)));
        cfgScaleTitle.setText(getString(R.string.app_generate_cfg_scale, 7f));

        if (BuildConfig.NSFW_ENABLED) {
            nsfwField.setVisibility(View.VISIBLE);
            nsfwField.setChecked(preferences.getBoolean(SharedPreferencesHelper.NSFW_TOGGLED, false));
            censorNsfwField.setVisibility(View.VISIBLE);
            censorNsfwField.setChecked(preferences.getBoolean(SharedPreferencesHelper.CENSOR_NSFW, false));
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
            karrasField.setChecked(request.getKarras());
        }

        aiImageProvider.getModels(response -> {
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
            cancelPreviewButton.setVisibility(View.VISIBLE);
        }, error -> {
            logger.error("AiHorde", "Fetching list of models failed", error);
            Toast.makeText(this, R.string.app_error_fetching_models_failed, LENGTH_LONG).show();
        });
    }

    private void createGenerateRequest(OnGenerateRequestCreated onCreated) {
        createGenerateRequest(onCreated, null, false);
    }

    private void createGenerateRequest(OnGenerateRequestCreated onCreated, OnGenerateRequestFailed onFailed) {
        createGenerateRequest(onCreated, onFailed, true);
    }

    private void createGenerateRequest(OnGenerateRequestCreated onCreated, @Nullable OnGenerateRequestFailed onFailed, boolean replace) {
        TextInputEditText prompt = findViewById(R.id.prompt_field);
        TextInputEditText negativePrompt = findViewById(R.id.negative_prompt_field);
        Spinner sampler = findViewById(R.id.sampler_field);
        Slider steps = findViewById(R.id.steps_slider);
        Slider clipSkip = findViewById(R.id.clip_skip_slider);
        TextInputEditText width = findViewById(R.id.width_field);
        TextInputEditText height = findViewById(R.id.height_field);
        Spinner upscaler = findViewById(R.id.upscaler);
        SwitchCompat nsfw = findViewById(R.id.nsfw_switch);
        SwitchCompat censorNsfw = findViewById(R.id.censor_nsfw_switch);
        Slider cfgScale = findViewById(R.id.cfg_scale);
        SwitchCompat hiresFix = findViewById(R.id.hires_fix_switch);
        SwitchCompat karras = findViewById(R.id.karras_switch);

        boolean advanced = ((SwitchCompat) findViewById(R.id.advanced_switch)).isChecked();

        int[] widthAndHeight = this.calculateWidthAndHeight();
        String defaultUpscaler = this.getUpscaler(widthAndHeight);

        String selectedUpscaler = (String) upscaler.getSelectedItem();
        String noneOption = getString(R.string.app_select_empty_option);

        String promptText = Objects.requireNonNull(prompt.getText()).toString();

        GenerateRequest request = new GenerateRequest(
                promptText,
                negativePrompt.getText().length() > 0 ? negativePrompt.getText().toString() : null,
                selectedModels,
                advanced ? Sampler.valueOf((String) sampler.getSelectedItem()) : Sampler.k_dpmpp_sde,
                advanced ? (int) steps.getValue() : 20,
                advanced ? (int) clipSkip.getValue() : 1,
                advanced && !width.getText().toString().equals("") ? Integer.parseInt(width.getText().toString()) : widthAndHeight[0],
                advanced && !height.getText().toString().equals("") ? Integer.parseInt(height.getText().toString()) : widthAndHeight[1],
                null,
                advanced ? (selectedUpscaler.equals(noneOption) ? null : selectedUpscaler) : defaultUpscaler,
                advanced ? cfgScale.getValue() : 7,
                BuildConfig.NSFW_ENABLED && nsfw.isChecked(),
                !advanced || karras.isChecked(),
                advanced && hiresFix.isChecked(),
                false,
                !BuildConfig.NSFW_ENABLED || censorNsfw.isChecked()
        );

        if (!replace) {
            onCreated.onCreated(request);
            return;
        }

        PromptReplacer.replacePrompt(this, promptText, result -> {
            if (result == null) {
                if (onFailed != null) {
                    onFailed.onFailed();
                }
                return;
            }

            onCreated.onCreated(GenerateRequestHelper.withPrompt(request, result));
        });
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        setButtonEnabled(button, enabled, R.color.md_theme_primary, R.color.md_theme_onPrimary);
    }

    private void setButtonEnabled(Button button, boolean enabled, @DrawableRes int enabledColor, @ColorRes int enabledTextColor) {
        button.setEnabled(enabled);
        button.setBackgroundResource(enabled ? enabledColor : android.R.color.darker_gray);
        button.setTextColor(getResources().getColor(enabled ? enabledTextColor : android.R.color.black, null));
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

    private void createCustomParameters(@NonNull PremadePrompt prompt) {
        if (prompt.getCustomParameters() == null || prompt.getCustomParameters().isEmpty()) {
            return;
        }

        ThreadHelper.runInThread(() -> {
            AppDatabase database = DatabaseHelper.getDatabase(this);

            for (PremadePromptCustomParameter promptCustomParameter : prompt.getCustomParameters()) {
                CustomParameterWithValues existingParameter = database.customParameters().findByName(promptCustomParameter.getName());
                if (existingParameter != null) {
                    if (existingParameter.values != null) {
                        for (CustomParameterValue value : existingParameter.values) {
                            database.customParameterValues().delete(value);
                        }
                    }
                    database.customParameters().delete(existingParameter);
                }
                CustomParameter newParameter = new CustomParameter();
                newParameter.name = promptCustomParameter.getName();
                newParameter.description = promptCustomParameter.getDescription();
                newParameter.expression = promptCustomParameter.getExpression();

                long newParameterId = database.customParameters().create(newParameter);
                for (PremadePromptCustomParameterCondition condition : promptCustomParameter.getConditions()) {
                    CustomParameterValue value = new CustomParameterValue(CustomParameterValue.ConditionType.valueOf(condition.getType().name()));
                    value.customParameterId = newParameterId;
                    value.value = condition.getValue();
                    value.expression = condition.getExpression();

                    database.customParameterValues().create(value);
                }
            }
        }, this);
    }
}