package cz.chrastecky.aiwallpaperchanger.provider;

import androidx.annotation.NonNull;

import java.util.concurrent.CompletableFuture;

public interface AiTextProvider {
    CompletableFuture<String> getResponse(String message);

    @NonNull
    String getId();
}
