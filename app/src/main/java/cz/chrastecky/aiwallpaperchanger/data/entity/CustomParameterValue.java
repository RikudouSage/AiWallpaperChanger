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
    public int customParameterId;

    public CustomParameterValue() {}
    public CustomParameterValue(
            String expression,
            String value,
            ConditionType type,
            int customParameterId
    ) {
        this.expression = expression;
        this.value = value;
        this.type = type;
        this.customParameterId = customParameterId;
    }
}
