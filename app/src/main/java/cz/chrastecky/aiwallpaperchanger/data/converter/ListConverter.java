package cz.chrastecky.aiwallpaperchanger.data.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class ListConverter {
    @TypeConverter
    public static String fromList(final List<String> list) {
        if (list == null) {
            return null;
        }

        return new Gson().toJson(list);
    }

    @TypeConverter
    public static List<String> toList(final String data) {
        if (data == null) {
            return null;
        }

        return new Gson().fromJson(data, new TypeToken<List<String>>() {}.getType());
    }
}
