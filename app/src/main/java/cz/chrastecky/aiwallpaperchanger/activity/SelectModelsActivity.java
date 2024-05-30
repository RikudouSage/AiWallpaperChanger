package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivitySelectModelsBinding;
import cz.chrastecky.aiwallpaperchanger.databinding.ModelItemBinding;

public class SelectModelsActivity extends AppCompatActivity {

    private List<String> allModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySelectModelsBinding binding = ActivitySelectModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_select_models);

        Intent intent = getIntent();
        List<String> selectedModels = intent.getStringArrayListExtra("selectedModels");
        allModels = intent.getStringArrayListExtra("allModels");

        assert selectedModels != null;
        assert allModels != null;

        redrawModels(selectedModels, binding);
    }

    private void redrawModels(List<String> selectedModels, ActivitySelectModelsBinding binding) {
        binding.setHasSelectedModels(!selectedModels.isEmpty());

        for (String model : selectedModels) {
            ModelItemBinding modelItem = ModelItemBinding.inflate(getLayoutInflater());
            modelItem.setModelName(model);
            modelItem.setDeleteButton(true);
            modelItem.actionButton.setOnClickListener(v -> {
                Toast.makeText(this, "Remove - " + modelItem.getModelName(), Toast.LENGTH_LONG).show();
            });
            binding.selectedModelsWrapper.addView(modelItem.getRoot());
        }

        List<String> availableModels = allModels.stream().filter(model -> !selectedModels.contains(model)).collect(Collectors.toList());
        for (String model : availableModels) {
            ModelItemBinding modelItem = ModelItemBinding.inflate(getLayoutInflater());
            modelItem.setModelName(model);
            modelItem.setDeleteButton(false);
            modelItem.actionButton.setOnClickListener(v -> {
                Toast.makeText(this, "Add - " + modelItem.getModelName(), Toast.LENGTH_LONG).show();
            });
            binding.availableModelsWrapper.addView(modelItem.getRoot());
        }
    }
}