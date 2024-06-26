package cz.chrastecky.aiwallpaperchanger.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class CustomParameterValue {
    public enum ConditionType {
        If,
        Else,
    }

    @PrimaryKey(autoGenerate = true)
    public int id;
    public String expression;
    public String value;
    public ConditionType type;
    public long customParameterId;

    public CustomParameterValue() {}
    public CustomParameterValue(ConditionType type) {
        this.type = type;
    }
}
