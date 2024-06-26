package cz.chrastecky.aiwallpaperchanger.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class CustomParameter {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String description;

    public CustomParameter() {}
    public CustomParameter(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
