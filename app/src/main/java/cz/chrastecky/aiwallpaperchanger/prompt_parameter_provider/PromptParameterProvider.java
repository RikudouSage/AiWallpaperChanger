package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;

public interface PromptParameterProvider {
    @NonNull
    CompletableFuture<List<String>> getParameterNames(@NonNull final Context context);

    @Nullable
    CompletableFuture<String> getValue(@NonNull final Context context, @NonNull final String parameterName);

    @NonNull
    String getDescription(@NonNull final Context context, @NonNull final String parameterName);

    @Nullable
    List<String> getRequiredPermissions(@NonNull final Context context, @NonNull List<String> grantedPermissions, @NonNull final String parameterName);

    default boolean permissionsSatisfied(@NonNull final Context context, @NonNull List<String> grantedPermissions, @NonNull final String parameterName) {
        final List<String> requiredPermissions = getRequiredPermissions(context, grantedPermissions, parameterName);
        if (requiredPermissions == null) {
            return true;
        }
        return new HashSet<>(grantedPermissions).containsAll(requiredPermissions);
    }

    default CompletableFuture<List<String>> getParametersInText(@NonNull Context context, @NonNull String... texts) {
        final CompletableFuture<List<String>> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final List<String> result = new ArrayList<>();
            for (String parameter : getParameterNames(context).join()) {
                for (final String text : texts) {
                    if (text != null && text.contains("${" + parameter + "}")) {
                        result.add(parameter);
                    }
                }
            }

            future.complete(result);
        }, context);

        return future;
    }
}
