package cz.chrastecky.aiwallpaperchanger.provider;

import android.annotation.SuppressLint;
import android.content.Context;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.ActiveModel;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestFullStatusImage;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestStatusCheck;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailImage;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationQueued;
import cz.chrastecky.aiwallpaperchanger.dto.response.HordeWarning;
import cz.chrastecky.aiwallpaperchanger.dto.response.ModelType;
import cz.chrastecky.aiwallpaperchanger.exception.ContentCensoredException;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;
import cz.chrastecky.aiwallpaperchanger.helper.ApiKeyHelper;
import cz.chrastecky.aiwallpaperchanger.helper.HashHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;

public class AiHorde implements AiImageProvider {
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
                BuildConfig.HORDE_API_URL + "/status/models",
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
                headers.put("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER);

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
            requestBody.put("extra_slow_workers", request.getExtraSlowWorkers());

            if (
                    BuildConfig.BILLING_ENABLED
                    && !BuildConfig.PREMIUM_API_KEY.equals(BuildConfig.ANONYMOUS_API_KEY)
                    && ApiKeyHelper.getDefaultApiKey().equals(BuildConfig.PREMIUM_API_KEY)
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
                BuildConfig.HORDE_API_URL + "/generate/async",
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
                headers.put("apikey", ApiKeyHelper.getApiKey(context));
                headers.put("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER);
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
                BuildConfig.HORDE_API_URL + "/generate/check/" + id,
                null,
                onResponse::onProgress,
                onError::onError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", ApiKeyHelper.getApiKey(context));
                headers.put("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER);
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
                BuildConfig.HORDE_API_URL + "/generate/status/" + id,
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
                headers.put("apikey", ApiKeyHelper.getApiKey(context));
                headers.put("Client-Agent", BuildConfig.CLIENT_AGENT_HEADER);
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

    @Nullable
    private String uniqueId() {
        @SuppressLint("HardwareIds")
        String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null) {
            return null;
        }
        return HashHelper.sha256(id, logger);
    }
}
