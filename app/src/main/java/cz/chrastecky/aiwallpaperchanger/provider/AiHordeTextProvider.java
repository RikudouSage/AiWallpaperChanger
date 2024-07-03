package cz.chrastecky.aiwallpaperchanger.provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateTextRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestFullStatusText;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationQueued;
import cz.chrastecky.aiwallpaperchanger.dto.response.TextModel;
import cz.chrastecky.aiwallpaperchanger.helper.ApiKeyHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.aiwallpaperchanger.text_formatter.MetaLlama3Instruct;
import cz.chrastecky.aiwallpaperchanger.text_formatter.TextFormatter;
import cz.chrastecky.annotationprocessor.InjectableTextProvider;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

@InjectableTextProvider
public class AiHordeTextProvider implements AiTextProvider {
    public static final String ID = "horde";

    private final Context context;
    private final Logger logger;

    public AiHordeTextProvider(Context context) {
        this.context = context;
        this.logger = new Logger(context);
    }

    @Override
    public CompletableFuture<String> getResponse(final String message) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final List<String> models = findModels().join();
            if (models.isEmpty()) {
                logger.error("AIHorde", "Failed to find any suitable text models");
                future.complete("");
                return;
            }

            final TextFormatter textFormatter = new MetaLlama3Instruct(); // todo make this dynamic

            final OkHttpClient httpClient = new OkHttpClient();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(BuildConfig.HORDE_API_URL + "/generate/text/async")
                    .addHeader("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER)
                    .addHeader("apikey", ApiKeyHelper.getApiKey(context))
                    .post(RequestBody.create(
                            new Gson().toJson(new GenerateTextRequest(textFormatter.encode(message), findModels().join())),
                            MediaType.parse("application/json; charset=utf-8")
                    ))
                    .build();
            try (final okhttp3.Response response = httpClient.newCall(request).execute()) {
                final GenerationQueued result = new Gson().fromJson(response.body().string(), GenerationQueued.class);
                final String requestId = result.getId();

                while (true) {
                    AsyncRequestFullStatusText status = getTextStatus(requestId).join();
                    if (status.getFaulted()) {
                        logger.error("AiHorde", "The text request faulted");
                        future.complete("");
                        return;
                    }
                    if (status.getDone()) {
                        if (status.getGenerations().isEmpty()) {
                            logger.error("AiHorde", "The text request result was empty even though it claimed completed");
                            future.complete("");
                            return;
                        }

                        future.complete(textFormatter.decode(status.getGenerations().get(0).getText()));
                        return;
                    }
                    Thread.sleep(2_000);
                }

            } catch (IOException e) {
                logger.error("AiHorde", "There was an error while generating text response", e);
                future.complete("");
            } catch (InterruptedException e) {
                logger.error("AiHorde", "Sleeping in a thread got interrupted", e);
                future.complete("");
            }
        }, context);

        return future;
    }

    @NonNull
    @Override
    public String getId() {
        return ID;
    }

    @NonNull
    private CompletableFuture<List<String>> findModels() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final List<String> wantedModels = Arrays.asList("l3", "llama3", "llama-3"); // add openhermes-2.5-mistral-7b
            final OkHttpClient httpClient = new OkHttpClient();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(BuildConfig.HORDE_API_URL + "/status/models?type=text")
                    .addHeader("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER)
                    .build();

            try (final okhttp3.Response response = httpClient.newCall(request).execute()) {
                List<TextModel> result = Arrays.asList(new Gson().fromJson(
                        response.body().string(),
                        TextModel[].class
                ));

                future.complete(
                        result.stream()
                                .map(TextModel::getName)
                                .filter(textModel -> wantedModels.stream().anyMatch(wantedModel -> textModel.toLowerCase().contains(wantedModel)))
                                .collect(Collectors.toList())
                );
            } catch (IOException e) {
                logger.error("AiHorde", "There was an error while fetching list of text models", e);
                future.complete(new ArrayList<>());
            }
        }, context);

        return future;
    }

    @NonNull
    private CompletableFuture<AsyncRequestFullStatusText> getTextStatus(@NonNull final String id) {
        final CompletableFuture<AsyncRequestFullStatusText> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final OkHttpClient httpClient = new OkHttpClient();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(BuildConfig.HORDE_API_URL + "/generate/text/status/" + id)
                    .addHeader("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER)
                    .addHeader("apikey", ApiKeyHelper.getApiKey(context))
                    .build();

            try (final okhttp3.Response response = httpClient.newCall(request).execute()) {
                final String body = response.body().string();
                future.complete(new Gson().fromJson(body, AsyncRequestFullStatusText.class));
            } catch (IOException e) {
                logger.error("AiHorde", "There was an error while getting the status of request with id " + id, e);
                future.complete(null);
            }
        }, context);

        return future;
    }
}
