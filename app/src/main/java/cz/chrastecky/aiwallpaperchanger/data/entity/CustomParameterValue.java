package cz.chrastecky.aiwallpaperchanger.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class CustomParameterValue implements Comparable<CustomParameterValue> {
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

    @Ignore
    public CustomParameterValue(ConditionType type) {
        this.type = type;
    }

    @Override
    public int compareTo(CustomParameterValue o) {
        if (type == o.type) {
            return 0;
        }

        if (type == ConditionType.If) {
            return -1;
        }

        return 1;
    }
}
