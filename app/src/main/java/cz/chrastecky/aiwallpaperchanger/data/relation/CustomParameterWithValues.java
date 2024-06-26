package cz.chrastecky.aiwallpaperchanger.data.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameter;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;

public class CustomParameterWithValues {
    @Embedded
    public CustomParameter customParameter;
    @Relation(
            parentColumn = "id",
            entityColumn = "customParameterId"
    )
    public List<CustomParameterValue> values;
}
