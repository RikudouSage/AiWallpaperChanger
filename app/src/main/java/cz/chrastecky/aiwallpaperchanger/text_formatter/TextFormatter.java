package cz.chrastecky.aiwallpaperchanger.text_formatter;

import androidx.annotation.NonNull;

public interface TextFormatter {
    boolean supports(@NonNull String model);
    @NonNull
    String encode(@NonNull String message);
    @NonNull
    String decode(@NonNull String message);
}
