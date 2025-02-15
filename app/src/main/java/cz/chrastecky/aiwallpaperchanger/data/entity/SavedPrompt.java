package cz.chrastecky.aiwallpaperchanger.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.dto.Sampler;

@Entity(indices = {
        @Index(value = "name", unique = true)
})
public class SavedPrompt {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String name;

    @NonNull
    public String prompt;
    @NonNull
    public List<String> models;
    @Nullable
    public String negativePrompt;

    @Nullable
    public Sampler sampler;
    @Nullable
    public Boolean karras;
    @Nullable
    public Integer steps;
    @Nullable
    public Integer clipSkip;
    @Nullable
    public Integer width;
    @Nullable
    public Integer height;
    @Nullable
    public String upscaler;
    @Nullable
    public Double cfgScale;
    @Nullable
    public Boolean hiresFix;

    public SavedPrompt(
        @NonNull final String name,
        @NonNull final String prompt,
        @NonNull final List<String> models
    ) {
        this.name = name;
        this.prompt = prompt;
        this.models = models;
    }
}
