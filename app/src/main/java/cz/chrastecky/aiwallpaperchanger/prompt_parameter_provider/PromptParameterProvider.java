package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PromptParameterProvider {
    @NonNull
    CompletableFuture<List<String>> getParameterNames(@NonNull final Context context);

    @Nullable
    CompletableFuture<String> getValue(@NonNull final Context context, @NonNull final String parameterName);

    @NonNull
    String getDescription(@NonNull final Context context, @NonNull final String parameterName);

    @Nullable
    List<String> getRequiredPermissions(@NonNull List<String> grantedPermissions, @NonNull final String parameterName);

    default boolean permissionsSatisfied(@NonNull List<String> grantedPermissions, @NonNull final String parameterName) {
        final List<String> requiredPermissions = getRequiredPermissions(grantedPermissions, parameterName);
        if (requiredPermissions == null) {
            return true;
        }
        return new HashSet<>(grantedPermissions).containsAll(requiredPermissions);
    }
}
