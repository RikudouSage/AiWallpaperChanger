package cz.chrastecky.aiwallpaperchanger.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivitySelectModelsBinding;

public class SelectModelsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivitySelectModelsBinding binding = ActivitySelectModelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_select_models);

        Intent intent = getIntent();
        List<String> selectedModels = intent.getStringArrayListExtra("selectedModels");
        List<String> allModels = intent.getStringArrayListExtra("allModels");

        assert selectedModels != null;
        assert allModels != null;

        binding.setSelectedModels(selectedModels);
        binding.setUnselectedModels(allModels.stream().filter(model -> !selectedModels.contains(model)).collect(Collectors.toList()));
    }
}