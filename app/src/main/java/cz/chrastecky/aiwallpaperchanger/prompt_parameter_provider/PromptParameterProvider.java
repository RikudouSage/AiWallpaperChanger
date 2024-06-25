package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PromptParameterProvider {
    @NonNull
    String getParameterName();
    @Nullable
    CompletableFuture<String> getValue(@NonNull final Context context);
    @NonNull
    String getDescription(@NonNull final Context context);
    @Nullable
    List<String> getRequiredPermissions(@NonNull List<String> grantedPermissions);
    default boolean permissionsSatisfied(@NonNull List<String> grantedPermissions) {
        final List<String> requiredPermissions = getRequiredPermissions(grantedPermissions);
        if (requiredPermissions == null) {
            return true;
        }
        return new HashSet<>(grantedPermissions).containsAll(requiredPermissions);
    }
}
