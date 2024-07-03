package cz.chrastecky.aiwallpaperchanger.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.provider.Settings;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateTextRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.ActiveModel;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestFullStatusImage;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestFullStatusText;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestStatusCheck;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailImage;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationQueued;
import cz.chrastecky.aiwallpaperchanger.dto.response.HordeWarning;
import cz.chrastecky.aiwallpaperchanger.dto.response.ModelType;
import cz.chrastecky.aiwallpaperchanger.dto.response.TextModel;
import cz.chrastecky.aiwallpaperchanger.exception.ContentCensoredException;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;
import cz.chrastecky.aiwallpaperchanger.helper.HashHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.aiwallpaperchanger.text_formatter.LlmTextFormatterProvider;
import cz.chrastecky.aiwallpaperchanger.text_formatter.TextFormatter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class AiHorde implements AiImageProvider, AiTextProvider {
    private static final String CLIENT_AGENT_HEADER = BuildConfig.APPLICATION_ID + ":" + BuildConfig.VERSION_NAME + ":" + BuildConfig.MAINTAINER;
    public static String DEFAULT_API_KEY = BuildConfig.API_KEY;

    private static final String baseUrl = "https://aihorde.net/api/v2";
    private final RequestQueue requestQueue;
    private final Context context;
    private final Logger logger;

    public AiHorde(Context context) {
        this.requestQueue = Volley.newRequestQueue(context);
        this.context = context;
        this.logger = new Logger(this.context);
    }

    @Override
    public void getModels(@NonNull OnResponse<List<ActiveModel>> onResponse, @Nullable OnError onError) {
        requestQueue.add(new JsonRequest<List<ActiveModel>>(
                Request.Method.GET,
                baseUrl + "/status/models",
                null,
                onResponse::onResponse,
                volleyError -> {
                    if (onError != null) {
                        onError.onError(volleyError);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Client-Agent", CLIENT_AGENT_HEADER);

                return headers;
            }

            @Override
            protected Response<List<ActiveModel>> parseNetworkResponse(NetworkResponse networkResponse) {
                String body = new String(networkResponse.data, StandardCharsets.UTF_8);
                try {
                    List<ActiveModel> result = new ArrayList<>();
                    JSONArray json = new JSONArray(body);
                    for (int i = 0; i < json.length(); ++i) {
                        JSONObject object = json.getJSONObject(i);
                        ActiveModel model = new ActiveModel(
                                object.getString("name"),
                                object.getInt("count"),
                                object.getDouble("performance"),
                                object.getInt("queued"),
                                object.getInt("jobs"),
                                object.getInt("eta"),
                                ModelType.valueOf(object.getString("type"))
                        );
                        if (!BuildConfig.NON_COMMERCIAL && model.getName().equals("Stable Cascade 1.0")) {
                            continue;
                        }
                        result.add(model);
                    }

                    return Response.success(result, HttpHeaderParser.parseCacheHeaders(networkResponse));
                } catch (JSONException e) {
                    return Response.error(new VolleyError(networkResponse));
                }
            }
        });
    }

    @Override
    public void generateImage(
            @NonNull GenerateRequest request,
            @NonNull OnProgress onProgress,
            @NonNull OnResponse<GenerationDetailWithBitmap> onResponse,
            @Nullable OnError onError
    ) {
        String prompt = request.getPrompt();
        if (request.getNegativePrompt() != null) {
            prompt += " ### " + request.getNegativePrompt();
        }
        JSONObject requestBody;
        try {
            List<String> postProcessors = new ArrayList<>();
            if (request.getFaceFixer() != null) {
                postProcessors.add(request.getFaceFixer().name());
            }
            if (request.getUpscaler() != null) {
                postProcessors.add(request.getUpscaler());
            }

            requestBody = new JSONObject();
            requestBody.put("prompt", prompt);

            JSONObject params = new JSONObject();
            params.put("sampler_name", request.getSampler().name());
            params.put("steps", request.getSteps());
            params.put("clip_skip", request.getClipSkip());
            params.put("width", request.getWidth());
            params.put("height", request.getHeight());
            params.put("post_processing", new JSONArray(postProcessors));
            params.put("cfg_scale", request.getCfgScale());
            params.put("karras", request.getKarras());
            params.put("hires_fix", request.getHiresFix());

            requestBody.put("params", params);
            requestBody.put("models", new JSONArray(request.getModels()));
            requestBody.put("nsfw", request.getNsfw());
            if (
                    BuildConfig.BILLING_ENABLED
                    && !BuildConfig.PREMIUM_API_KEY.equals(BuildConfig.ANONYMOUS_API_KEY)
                    && DEFAULT_API_KEY.equals(BuildConfig.PREMIUM_API_KEY)
            ) {
                requestBody.put("proxied_account", uniqueId() == null ? "unknown" : uniqueId());
            }
        } catch (JSONException e) {
            if (onError != null) {
                onError.onError(new VolleyError(e));
            }
            return;
        }

        requestQueue.add(new JsonRequest<GenerationQueued>(
                Request.Method.POST,
                baseUrl + "/generate/async",
                requestBody.toString(),
                generationQueued -> {
                    String id = generationQueued.getId();
                    recursivelyCheckForStatus(id, onProgress, onResponse, onError);
                },
                volleyError -> {
                    if (volleyError.networkResponse != null) {
                        logger.debug("HordeError", new String(volleyError.networkResponse.data));
                    } else if (volleyError.getMessage() != null) {
                        logger.debug("HordeError", volleyError.getMessage());
                    } else if (volleyError.getCause() != null) {
                        logger.debug("HordeError", volleyError.getCause().getMessage(), volleyError.getCause());
                    } else {
                        logger.debug("HordeError", volleyError.toString());
                    }
                    if (onError != null) {
                        onError.onError(volleyError);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", apiKey());
                headers.put("Client-Agent", CLIENT_AGENT_HEADER);
                return headers;
            }

            @Override
            protected Response<GenerationQueued> parseNetworkResponse(NetworkResponse networkResponse) {
                String body = new String(networkResponse.data, StandardCharsets.UTF_8);
                try {
                    JSONObject object = new JSONObject(body);

                    List<HordeWarning> warnings = null;
                    if (object.has("warnings")) {
                        warnings = new ArrayList<>();
                        JSONArray rawWarnings = object.getJSONArray("warnings");
                        for (int i = 0; i < rawWarnings.length(); ++i) {
                            JSONObject warning = rawWarnings.getJSONObject(i);
                            warnings.add(new HordeWarning(warning.getString("code"), warning.getString("message")));
                        }
                    }

                    return Response.success(
                            new GenerationQueued(
                                    object.getString("id"),
                                    object.getInt("kudos"),
                                    object.has("message") ? object.getString("message") : null,
                                    warnings
                            ),
                            HttpHeaderParser.parseCacheHeaders(networkResponse)
                    );
                } catch (JSONException e) {
                    return Response.error(new VolleyError(networkResponse));
                }
            }
        });
    }

    private JsonRequest<AsyncRequestStatusCheck> getCheckStatusRequest(String id, OnProgress onResponse, OnError onError) {
        return new JsonRequest<AsyncRequestStatusCheck>(
                Request.Method.GET,
                baseUrl + "/generate/check/" + id,
                null,
                onResponse::onProgress,
                onError::onError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", apiKey());
                headers.put("Client-Agent", CLIENT_AGENT_HEADER);
                return headers;
            }

            @Override
            protected Response<AsyncRequestStatusCheck> parseNetworkResponse(NetworkResponse networkResponse) {
                try {
                    String body = new String(networkResponse.data, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(body);

                    return Response.success(
                            new AsyncRequestStatusCheck(
                                    json.getInt("finished"),
                                    json.getInt("processing"),
                                    json.getInt("restarted"),
                                    json.getInt("waiting"),
                                    json.getBoolean("done"),
                                    json.getBoolean("faulted"),
                                    json.getInt("wait_time"),
                                    json.getInt("queue_position"),
                                    json.getInt("kudos"),
                                    json.getBoolean("is_possible")
                            ),
                            HttpHeaderParser.parseCacheHeaders(networkResponse)
                    );
                } catch (JSONException e) {
                    return Response.error(new VolleyError(networkResponse));
                }
            }
        };
    }

    private JsonRequest<AsyncRequestFullStatusImage> getFullStatusRequest(String id, OnResponse<GenerationDetailImage> onResponse, OnError onError) {
        return new JsonRequest<AsyncRequestFullStatusImage>(
                Request.Method.GET,
                baseUrl + "/generate/status/" + id,
                null,
                asyncRequestFullStatus -> {
                    if (asyncRequestFullStatus.getGenerations().isEmpty()) {
                        onError.onError(new VolleyError(new RetryGenerationException()));
                        return;
                    }
                    if (asyncRequestFullStatus.getGenerations().get(0).getCensored()) {
                        onError.onError(new VolleyError(new ContentCensoredException()));
                        return;
                    }
                    onResponse.onResponse(asyncRequestFullStatus.getGenerations().get(0));
                },
                onError::onError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", apiKey());
                headers.put("Client-Agent", CLIENT_AGENT_HEADER);
                return headers;
            }

            @Override
            protected Response<AsyncRequestFullStatusImage> parseNetworkResponse(NetworkResponse networkResponse) {
                try {
                    String body = new String(networkResponse.data, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(body);
                    JSONArray generationsRaw = json.getJSONArray("generations");
                    List<GenerationDetailImage> generations = new ArrayList<>();
                    for (int i = 0; i < generationsRaw.length(); ++i) {
                        JSONObject generationJson = generationsRaw.getJSONObject(i);
                        generations.add(new GenerationDetailImage(
                                generationJson.getString("worker_id"),
                                generationJson.getString("worker_name"),
                                generationJson.getString("model"),
                                generationJson.getString("state"),
                                generationJson.getString("img"),
                                generationJson.getString("seed"),
                                generationJson.getString("id"),
                                generationJson.getBoolean("censored")
                        ));
                    }

                    return Response.success(
                            new AsyncRequestFullStatusImage(
                                    json.getInt("finished"),
                                    json.getInt("processing"),
                                    json.getInt("restarted"),
                                    json.getInt("waiting"),
                                    json.getBoolean("done"),
                                    json.getBoolean("faulted"),
                                    json.getInt("wait_time"),
                                    json.getInt("queue_position"),
                                    json.getInt("kudos"),
                                    json.getBoolean("is_possible"),
                                    generations,
                                    json.getBoolean("shared")
                            ),
                            HttpHeaderParser.parseCacheHeaders(networkResponse)
                    );
                } catch (JSONException e) {
                    return Response.error(new VolleyError(networkResponse));
                }
            }
        };
    }

    private void recursivelyCheckForStatus(String id, OnProgress onProgress, OnResponse<GenerationDetailWithBitmap> onResponse, @Nullable OnError onError) {
        requestQueue.add(getCheckStatusRequest(
                id,
                progress -> {
                    if (!progress.getDone()) {
                        onProgress.onProgress(progress);
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                AiHorde.this.recursivelyCheckForStatus(id, onProgress, onResponse, onError);
                            }
                        }, 2_000);
                        return;
                    }

                    requestQueue.add(getFullStatusRequest(id, status -> {
                        requestQueue.add(new ImageRequest(status.getImg(), detail -> {
                            onResponse.onResponse(new GenerationDetailWithBitmap(status, detail));
                        }, 1_000_000, 1_000_000, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565, volleyError -> {
                            if (onError != null) {
                                onError.onError(volleyError);
                            }
                        }));
                    }, error -> {
                        if (onError != null) {
                            onError.onError(error);
                        }
                    }));
                },
                error -> {
                    if (onError != null) {
                        onError.onError(error);
                    }
                }
        ));
    }

    private String apiKey() {
        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
        return preferences.getString(SharedPreferencesHelper.API_KEY, DEFAULT_API_KEY);
    }

    @Nullable
    private String uniqueId() {
        @SuppressLint("HardwareIds")
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null) {
            return null;
        }
        return HashHelper.sha256(id, logger);
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

            final LlmTextFormatterProvider textFormatterProvider = new LlmTextFormatterProvider();
            final TextFormatter textFormatter = textFormatterProvider.findForModel(models.get(0)); // todo make it actually support multiple models
            if (textFormatter == null) {
                logger.error("AIHorde", "Failed to find text formatter");
                future.complete("");
                return;
            }
            final OkHttpClient httpClient = new OkHttpClient();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(baseUrl + "/generate/text/async")
                    .addHeader("Client-Agent", CLIENT_AGENT_HEADER)
                    .addHeader("apikey", apiKey())
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
    private CompletableFuture<List<String>> findModels() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        ThreadHelper.runInThread(() -> {
            final List<String> wantedModels = Arrays.asList("l3", "llama3", "llama-3"); // add openhermes-2.5-mistral-7b
            final OkHttpClient httpClient = new OkHttpClient();
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(baseUrl + "/status/models?type=text")
                    .addHeader("Client-Agent", CLIENT_AGENT_HEADER)
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
                    .url(baseUrl + "/generate/text/status/" + id)
                    .addHeader("Client-Agent", CLIENT_AGENT_HEADER)
                    .addHeader("apikey", apiKey())
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
