package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.helper.PromptReplacer;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class TopographyParameterProvider implements PromptParameterProvider {
    private final GeolocationDataParameterProvider geolocationDataProvider = new GeolocationDataParameterProvider();

    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("topography"));
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull Context context, @NonNull String parameterName) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> PromptReplacer.replacePrompt(
                context,
                "${llm:list 5 comma-separated tags/words describing the topography of ${town} in ${state} in ${country}, include only the tags separated by comma and nothing else}",
                future::complete
        ), context);

        return future;
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_topography_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return geolocationDataProvider.getRequiredPermissions(context, grantedPermissions, "town");
    }

    @Override
    public boolean permissionsSatisfied(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return geolocationDataProvider.permissionsSatisfied(context, grantedPermissions, "town");
    }
}
