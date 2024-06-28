package cz.chrastecky.aiwallpaperchanger.data.relation;

import androidx.annotation.Nullable;
import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.Collections;
import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;

public class CustomParameterWithValues {

    @Nullable
    @Embedded
    public CustomParameter customParameter;

    @Nullable
    @Relation(
            parentColumn = "id",
            entityColumn = "customParameterId"
    )
    public List<CustomParameterValue> values;

    public CustomParameterWithValues() {}

    public void sortValues() {
        if (values != null) {
            Collections.sort(values);
        }
    }
}
