package cz.chrastecky.aiwallpaperchanger.background;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;
import cz.chrastecky.aiwallpaperchanger.helper.History;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.horde.AiHorde;

public class GenerateAndSetBackgroundWorker extends ListenableWorker {
    public GenerateAndSetBackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @SuppressLint("ApplySharedPref")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            Log.d("WorkerJob", "Inside doWork()");
            AiHorde aiHorde = new AiHorde(getApplicationContext());
            SharedPreferences preferences = new SharedPreferencesHelper().get(getApplicationContext());

            if (!preferences.contains("generationParameters")) {
                completer.set(Result.failure());
                return "GenerationParametersNotFound";
            }

            String requestJson = preferences.getString("generationParameters", "");
            Log.d("WorkerJob", "Request: " + requestJson);
            GenerateRequest request = new Gson().fromJson(preferences.getString("generationParameters", ""), GenerateRequest.class);
            if (!BuildConfig.NSFW_ENABLED && request.getNsfw()) {
                request = new GenerateRequest(
                        request.getPrompt(),
                        request.getNegativePrompt(),
                        request.getModel(),
                        request.getSampler(),
                        request.getSteps(),
                        request.getClipSkip(),
                        request.getWidth(),
                        request.getHeight(),
                        request.getFaceFixer(),
                        request.getUpscaler(),
                        request.getCfgScale(),
                        false,
                        request.getKarras()
                );
            }
            GenerateRequest finalRequest = request;
            aiHorde.generateImage(request, status -> {
                Log.d("WorkerJob", "OnProgress");
            }, response -> {
                Log.d("WorkerJob", "Finished");
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                try {
                    wallpaperManager.setBitmap(response.getImage());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("lastChanged", DateFormat.getInstance().format(Calendar.getInstance().getTime()));
                    editor.commit();

                    History history = new History(getApplicationContext());
                    history.addItem(new StoredRequest(
                            UUID.randomUUID(),
                            finalRequest,
                            response.getDetail().getSeed(),
                            response.getDetail().getWorkerId(),
                            response.getDetail().getWorkerName(),
                            new Date()
                    ));

                    completer.set(Result.success());
                } catch (IOException e) {
                    Log.e("AIWallpaperError", "Failed setting new wallpaper", e);
                    completer.setException(e);
                }
            }, error -> {
                Log.e("AIWallpaperError", "Failed generating AI image", error);
                if (error.networkResponse != null) {
                    Log.d("AIWallpaperError", new String(error.networkResponse.data));
                } else {
                    Log.d("AIWallpaperError", error.getMessage(), error.getCause());
                }
                completer.setException(error);
            });

            return "JobCompleted";
        });
    }
}
