package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.data.AppDatabase;
import cz.chrastecky.aiwallpaperchanger.data.relation.CustomParameterWithValues;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityCustomParameterListBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.CustomParameterItemBinding;
import cz.chrastecky.aiwallpaperchanger.helper.DatabaseHelper;

public class CustomParameterListActivity extends AppCompatActivity {

    private ActivityCustomParameterListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustomParameterListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_settings_custom_parameters);

        binding.addButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AddOrEditCustomParameterActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.loader.setVisibility(View.VISIBLE);
        binding.noParameters.setVisibility(View.GONE);
        binding.rootView.removeAllViews();

        new Thread(() -> {
            AppDatabase database = DatabaseHelper.getDatabase(this);
            List<CustomParameterWithValues> parameters = database.customParameters().getAll();

            AtomicBoolean isEmpty = new AtomicBoolean(true);
            for (CustomParameterWithValues parameter : parameters) {
                isEmpty.set(false);
                runOnUiThread(() -> {
                    CustomParameterItemBinding itemBinding = CustomParameterItemBinding.inflate(getLayoutInflater());

                    itemBinding.setName(parameter.customParameter.name);
                    itemBinding.setDescription(parameter.customParameter.description);

                    binding.rootView.addView(itemBinding.getRoot());
                });
            }

            runOnUiThread(() -> {
                binding.noParameters.setVisibility(isEmpty.get() ? View.VISIBLE : View.GONE);
                binding.loader.setVisibility(View.GONE);
            });
        }).start();
    }
}