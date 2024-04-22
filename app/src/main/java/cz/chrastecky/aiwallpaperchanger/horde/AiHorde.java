package cz.chrastecky.aiwallpaperchanger.horde;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

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

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.ActiveModel;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestFullStatus;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestStatusCheck;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetail;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationQueued;
import cz.chrastecky.aiwallpaperchanger.dto.response.HordeWarning;
import cz.chrastecky.aiwallpaperchanger.dto.response.ModelType;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;

public class AiHorde {
    public interface OnResponse<T> {
        void onResponse(T response);
    }
    public interface OnError {
        void onError(VolleyError error);
    }

    public interface OnProgress {
        void onProgress(AsyncRequestStatusCheck status);
    }

    private static final String baseUrl = "https://aihorde.net/api/v2";
    private final RequestQueue requestQueue;
    private final Context context;

    public AiHorde(Context context) {
        this.requestQueue = Volley.newRequestQueue(context);
        this.context = context;
    }

    public void getModels(OnResponse<List<ActiveModel>> onResponse, @Nullable OnError onError) {
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
                        result.add(model);
                    }

                    return Response.success(result, HttpHeaderParser.parseCacheHeaders(networkResponse));
                } catch (JSONException e) {
                    return Response.error(new VolleyError(networkResponse));
                }
            }
        });
    }

    public void generateImage(
            GenerateRequest request,
            OnProgress onProgress,
            OnResponse<GenerationDetailWithBitmap> onResponse,
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

            requestBody.put("params", params);
            requestBody.put("models", new JSONArray(new String[] {request.getModel()}));
            requestBody.put("nsfw", request.getNsfw());
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
                        Log.d("HordeError", new String(volleyError.networkResponse.data));
                    } else {
                        Log.d("HordeError", volleyError.getMessage());
                    }
                    if (onError != null) {
                        onError.onError(volleyError);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", BuildConfig.API_KEY);
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
                headers.put("apikey", BuildConfig.API_KEY);
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

    private JsonRequest<AsyncRequestFullStatus> getFullStatusRequest(String id, OnResponse<GenerationDetail> onResponse, OnError onError) {
        return new JsonRequest<AsyncRequestFullStatus>(
                Request.Method.GET,
                baseUrl + "/generate/status/" + id,
                null,
                asyncRequestFullStatus -> {
                    if (asyncRequestFullStatus.getGenerations().isEmpty()) {
                        onError.onError(new VolleyError(new RetryGenerationException()));
                        return;
                    }
                    onResponse.onResponse(asyncRequestFullStatus.getGenerations().get(0));
                },
                onError::onError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("apikey", BuildConfig.API_KEY);
                return headers;
            }

            @Override
            protected Response<AsyncRequestFullStatus> parseNetworkResponse(NetworkResponse networkResponse) {
                try {
                    String body = new String(networkResponse.data, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(body);
                    JSONArray generationsRaw = json.getJSONArray("generations");
                    List<GenerationDetail> generations = new ArrayList<>();
                    for (int i = 0; i < generationsRaw.length(); ++i) {
                        JSONObject generationJson = generationsRaw.getJSONObject(i);
                        generations.add(new GenerationDetail(
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
                            new AsyncRequestFullStatus(
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
                        try {
                            Thread.sleep(2000);
                            this.recursivelyCheckForStatus(id, onProgress, onResponse, onError);
                            return;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
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
}
