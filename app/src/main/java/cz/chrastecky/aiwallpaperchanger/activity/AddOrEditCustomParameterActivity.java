package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.data.relation.CustomParameterWithValues;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityAddOrEditCustomParameterBinding;
import cz.chrastecky.aiwallpaperchanger.helper.DatabaseHelper;

public class AddOrEditCustomParameterActivity extends AppCompatActivity {
    private ActivityAddOrEditCustomParameterBinding binding;
    private boolean isNew;
    private CustomParameterWithValues model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddOrEditCustomParameterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent mainIntent = getIntent();
        isNew = !mainIntent.hasExtra("id");

        setSupportActionBar(binding.toolbar);
        setTitle(isNew ? R.string.app_custom_parameters_create_title : R.string.app_custom_parameters_edit_title);

        if (isNew) {
            model = new CustomParameterWithValues();
            onLoad();
        } else {
            new Thread(() -> {
                model = DatabaseHelper.getDatabase(this).customParameters().find(mainIntent.getIntExtra("id", 0));
                if (model == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.app_custom_parameters_parameter_not_found, Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }

                runOnUiThread(this::onLoad);
            }).start();
        }
    }

    private void onLoad() {
        binding.loader.setVisibility(View.GONE);
    }
}