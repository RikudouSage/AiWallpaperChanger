package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.data.AppDatabase;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;
import cz.chrastecky.aiwallpaperchanger.data.relation.CustomParameterWithValues;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityAddOrEditCustomParameterBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.CustomParameterConditionItemBinding;
import cz.chrastecky.aiwallpaperchanger.helper.DatabaseHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public class AddOrEditCustomParameterActivity extends AppCompatActivity {
    private ActivityAddOrEditCustomParameterBinding binding;
    private boolean isNew;
    private CustomParameterWithValues model;

    private final Map<String, Boolean> formElementsValidation = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddOrEditCustomParameterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent mainIntent = getIntent();
        isNew = !mainIntent.hasExtra("id");

        setSupportActionBar(binding.toolbar);
        setTitle(isNew ? R.string.app_custom_parameters_create_title : R.string.app_custom_parameters_edit_title);

        binding.saveButton.setVisibility(View.GONE);

        if (isNew) {
            model = new CustomParameterWithValues();
            model.values = new ArrayList<>(Collections.singletonList(new CustomParameterValue(CustomParameterValue.ConditionType.Else)));
            onLoad();
        } else {
            ThreadHelper.runInThread(() -> {
                model = DatabaseHelper.getDatabase(this).customParameters().find(mainIntent.getIntExtra("id", 0));
                if (model == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.app_custom_parameters_parameter_not_found, Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                if (model.values == null) {
                    model.values = new ArrayList<>();
                }
                if (model.values.isEmpty()) {
                    model.values.add(new CustomParameterValue(CustomParameterValue.ConditionType.Else));
                }

                runOnUiThread(this::onLoad);
            }, this);
        }

        binding.saveButton.setOnClickListener(view -> {
            binding.loader.setVisibility(View.VISIBLE);
            binding.rootView.setVisibility(View.INVISIBLE);

            ThreadHelper.runInThread(() -> {
                assert model.values != null;
                assert !model.values.isEmpty();

                AppDatabase database = DatabaseHelper.getDatabase(this);
                long id = database.customParameters().upsert(model.customParameter);
                if (!isNew) {
                    assert model.customParameter != null;
                    id = model.customParameter.id;
                }

                for (CustomParameterValue value : model.values) {
                    value.customParameterId = id;
                }
                database.customParameterValues().upsertMultiple(model.values);
                runOnUiThread(this::finish);
            }, this);
        });

        binding.addConditionButton.setOnClickListener(view -> {
            assert model.values != null;
            assert !model.values.isEmpty();

            final int index = model.values.size() - 1;
            model.values.add(index, new CustomParameterValue(CustomParameterValue.ConditionType.If));

            renderConditions();
        });
        binding.removeConditionButton.setOnClickListener(view -> {
            assert model.values != null;
            assert !model.values.isEmpty();

            if (model.values.size() == 1) {
                return;
            }

            model.values.remove(model.values.size() - 2);
            renderConditions();
        });
    }

    private void onLoad() {
        model.sortValues();
        initializeForm();
        binding.loader.setVisibility(View.GONE);
        binding.rootView.setVisibility(View.VISIBLE);
    }

    private void initializeForm() {
        binding.saveButton.setVisibility(View.VISIBLE);

        formElementsValidation.put("name", model.customParameter != null && model.customParameter.name != null && !model.customParameter.name.isEmpty());
        formElementsValidation.put("expression", model.customParameter != null && model.customParameter.expression != null && !model.customParameter.expression.isEmpty());

        if (model.customParameter != null) {
            binding.parameterName.setText(model.customParameter.name);
            binding.parameterDescription.setText(model.customParameter.description);
            binding.parameterExpression.setText(model.customParameter.expression);
        }

        binding.parameterName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (model.customParameter == null) {
                    model.customParameter = new CustomParameter();
                }
                model.customParameter.name = s.toString();
                formElementsValidation.put("name", !model.customParameter.name.isEmpty());
                validateForm();

                if (model.customParameter.name.startsWith(" ") || model.customParameter.name.endsWith(" ")) {
                    binding.parameterNameWhitespaceWarning.setVisibility(View.VISIBLE);
                } else {
                    binding.parameterNameWhitespaceWarning.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.parameterDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (model.customParameter == null) {
                    model.customParameter = new CustomParameter();
                }
                model.customParameter.description = s.toString();
                validateForm();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.parameterExpression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (model.customParameter == null) {
                    model.customParameter = new CustomParameter();
                }
                model.customParameter.expression = s.toString();
                formElementsValidation.put("expression", !model.customParameter.expression.isEmpty());
                validateForm();

                if (model.customParameter.expression.startsWith(" ") || model.customParameter.expression.endsWith(" ")) {
                    binding.expressionWhitespaceWarning.setVisibility(View.VISIBLE);
                } else {
                    binding.expressionWhitespaceWarning.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        assert model.values != null;
        for (CustomParameterValue value : model.values) {
            addCondition(value);
        }

        validateForm();
    }

    private void renderConditions() {
        assert model.values != null;

        binding.conditionWrapper.removeAllViews();
        for (CustomParameterValue value : model.values) {
            addCondition(value);
        }
    }

    private void addCondition(CustomParameterValue value) {
        CustomParameterConditionItemBinding itemBinding = CustomParameterConditionItemBinding.inflate(getLayoutInflater());
        itemBinding.setModel(value);
        itemBinding.ifField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                value.expression = s.toString().isEmpty() ? null : s.toString();
                if (value.expression.startsWith(" ") || value.expression.endsWith(" ")) {
                    itemBinding.ifWhitespaceWarning.setVisibility(View.VISIBLE);
                } else {
                    itemBinding.ifWhitespaceWarning.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        itemBinding.valueField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                value.value = s.toString();
                if (value.value.startsWith(" ") || value.value.endsWith(" ")) {
                    itemBinding.valueWhitespaceWarning.setVisibility(View.VISIBLE);
                } else {
                    itemBinding.valueWhitespaceWarning.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.conditionWrapper.addView(itemBinding.getRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void validateForm() {
        for (String key : formElementsValidation.keySet()) {
            if (Boolean.FALSE.equals(formElementsValidation.get(key))) {
                setButtonEnabled(binding.saveButton, false);
                return;
            }
        }

        setButtonEnabled(binding.saveButton, true);
    }

    private void setButtonEnabled(@NonNull FloatingActionButton button, boolean enabled) {
        button.setEnabled(enabled);

        Drawable sourceDrawable = isNew
                ? AppCompatResources.getDrawable(this, android.R.drawable.ic_input_add)
                : AppCompatResources.getDrawable(this, android.R.drawable.ic_menu_save);
        assert sourceDrawable != null;

        if (enabled) {
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.md_theme_primary)));
            button.setImageDrawable(sourceDrawable);
        } else {
            Drawable newDrawable = Objects.requireNonNull(sourceDrawable.getConstantState()).newDrawable();
            newDrawable.mutate().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_ATOP);
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(android.R.color.darker_gray)));
            button.setImageDrawable(newDrawable);
        }
    }
}