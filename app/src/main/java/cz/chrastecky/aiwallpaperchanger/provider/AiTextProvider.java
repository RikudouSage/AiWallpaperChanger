package cz.chrastecky.aiwallpaperchanger.provider;

import java.util.concurrent.CompletableFuture;

public interface AiTextProvider {
    CompletableFuture<String> getResponse(String message);
}
