package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.PromptParameterProviders;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.data.AppDatabase;
import cz.chrastecky.aiwallpaperchanger.data.entity.CustomParameterValue;
import cz.chrastecky.aiwallpaperchanger.data.relation.CustomParameterWithValues;
import cz.chrastecky.aiwallpaperchanger.helper.DatabaseHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PromptReplacer;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class CustomParametersProvider implements PromptParameterProvider {
    private List<CustomParameterWithValues> parameters;

    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull final Context context) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            AppDatabase database = DatabaseHelper.getDatabase(context);
            parameters = database.customParameters().getAll();
            future.complete(parameters.stream().map(
                    parameter -> Objects.requireNonNull(parameter.customParameter).name
            ).collect(Collectors.toList()));
        }, context);

        return future;
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull final Context context, @NonNull final String parameterName) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final Logger logger = new Logger(context);

            final CustomParameterWithValues parameter = findByName(parameterName, logger);
            if (parameter == null) {
                logger.error("CustomParameter", "The parameter does not exist: " + parameterName);
                return;
            }

            assert parameter.customParameter != null;
            assert parameter.values != null;

            if (parameter.customParameter.expression == null) {
                parameter.customParameter.expression = "";
            }

            PromptReplacer.replacePrompt(context, parameter.customParameter.expression, expression -> {
                parameter.sortValues();
                for (CustomParameterValue value : parameter.values) {
                    if (value.type == CustomParameterValue.ConditionType.Else || value.expression.equals(expression)) {
                        if (value.value == null) {
                            value.value = "";
                        }
                        PromptReplacer.replacePrompt(context, value.value, future::complete);
                        return;
                    }
                }

                // this should not happen, else block is always added, but just in case
                logger.error("CustomParameters", "No else block was present with the parameter, returning empty value");
                future.complete("");
            });
        }, context);

        return future;
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull final String parameterName) {
        final Logger logger = new Logger(context);
        final CustomParameterWithValues parameter = findByName(parameterName, logger);
        if (parameter == null) {
            return context.getString(R.string.app_custom_parameters_no_description);
        }

        assert parameter.customParameter != null;
        return parameter.customParameter.description == null || parameter.customParameter.description.isEmpty() ? context.getString(R.string.app_custom_parameters_no_description) : parameter.customParameter.description;
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        final Map<PromptParameterProvider, List<String>> usedProviders = getUsedProviders(parameterName, context);

        if (usedProviders.isEmpty()) {
            return Collections.emptyList();
        }

        final Set<String> permissions = new HashSet<>();
        for (PromptParameterProvider provider : usedProviders.keySet()) {
            final List<String> parameterNames = Objects.requireNonNull(usedProviders.get(provider));
            for (String providerParameterName : parameterNames) {
                final List<String> parameterPermissions = provider.getRequiredPermissions(context, grantedPermissions, providerParameterName);
                if (parameterPermissions == null) {
                    continue;
                }
                permissions.addAll(parameterPermissions);
            }
        }

        return new ArrayList<>(permissions);
    }

    @Override
    public boolean permissionsSatisfied(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        final Map<PromptParameterProvider, List<String>> usedProviders = getUsedProviders(parameterName, context);

        for (PromptParameterProvider provider : usedProviders.keySet()) {
            final List<String> parameterNames = Objects.requireNonNull(usedProviders.get(provider));
            for (String providerParameterName : parameterNames) {
                if (!provider.permissionsSatisfied(context, grantedPermissions, providerParameterName)) {
                    return false;
                }
            }
        }

        return true;
    }

    @NonNull
    private Map<PromptParameterProvider, List<String>> getUsedProviders(@NonNull final String parameterName, @NonNull final Context context) {
        final Logger logger = new Logger(context);

        final CustomParameterWithValues parameter = findByName(parameterName, logger);
        if (parameter == null) {
            return new HashMap<>();
        }

        assert parameter.customParameter != null;
        final PromptParameterProviders providers = new PromptParameterProviders();

        final Map<PromptParameterProvider, List<String>> usedProviders = new HashMap<>();

        for (PromptParameterProvider provider : providers.getProviders()) {
            if (provider.getClass().equals(this.getClass())) {
                continue;
            }
            for (String providerParameterName : provider.getParameterNames(context).join()) {
                if (parameter.customParameter.expression.contains("${" + providerParameterName + "}")) {
                    if (!usedProviders.containsKey(provider)) {
                        usedProviders.put(provider, new ArrayList<>());
                    }

                    Objects.requireNonNull(usedProviders.get(provider)).add(providerParameterName);
                }
            }
        }

        return usedProviders;
    }

    @Nullable
    private CustomParameterWithValues findByName(@NonNull final String name, @NonNull final Logger logger) {
        if (parameters == null) {
            logger.error("CustomParameters", "The custom parameters list is null");
            return null;
        }

        List<CustomParameterWithValues> filtered = parameters.stream()
                .filter(parameter -> Objects.requireNonNull(parameter.customParameter).name.equals(name))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            return null;
        }

        return filtered.get(0);
    }
}
