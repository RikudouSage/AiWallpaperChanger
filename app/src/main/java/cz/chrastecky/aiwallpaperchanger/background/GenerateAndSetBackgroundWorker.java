package cz.chrastecky.aiwallpaperchanger.background;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.activity.PremiumActivity;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.exception.ContentCensoredException;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;
import cz.chrastecky.aiwallpaperchanger.helper.BillingHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ContentResolverHelper;
import cz.chrastecky.aiwallpaperchanger.helper.GenerateRequestMigrationHelper;
import cz.chrastecky.aiwallpaperchanger.helper.History;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ValueWrapper;
import cz.chrastecky.aiwallpaperchanger.provider.AiHorde;
import cz.chrastecky.aiwallpaperchanger.provider.AiProvider;

public class GenerateAndSetBackgroundWorker extends ListenableWorker {
    Logger logger = new Logger(getApplicationContext());

    public GenerateAndSetBackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @SuppressLint("ApplySharedPref")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> logger.error("UncaughtException", "Got uncaught exception", error)
        );

        return CallbackToFutureAdapter.getFuture(completer -> {
            BillingHelper.getPurchaseStatus(getApplicationContext(), PremiumActivity.PREMIUM_PURCHASE_NAME, premiumStatus -> {
                logger.debug("WorkerJob", "Is premium: " + (premiumStatus ? "Yes" : "No"));
                if (premiumStatus) {
                    AiHorde.DEFAULT_API_KEY = BuildConfig.PREMIUM_API_KEY;
                }

                logger.debug("WorkerJob", "Inside doWork()");
                AiHorde aiHorde = new AiHorde(getApplicationContext());
                SharedPreferences preferences = new SharedPreferencesHelper().get(getApplicationContext());

                if (!preferences.contains("generationParameters")) {
                    completer.set(Result.failure());
                    return;
                }

                String requestJson = preferences.getString("generationParameters", "");
                logger.debug("WorkerJob", "Request: " + requestJson);
                GenerateRequest request = GenerateRequestMigrationHelper.parse(preferences.getString("generationParameters", ""));
                if (!BuildConfig.NSFW_ENABLED && request.getNsfw()) {
                    request = new GenerateRequest(
                            request.getPrompt(),
                            request.getNegativePrompt(),
                            request.getModels(),
                            request.getSampler(),
                            request.getSteps(),
                            request.getClipSkip(),
                            request.getWidth(),
                            request.getHeight(),
                            request.getFaceFixer(),
                            request.getUpscaler(),
                            request.getCfgScale(),
                            false,
                            request.getKarras(),
                            request.getHiresFix()
                    );
                }
                final GenerateRequest finalRequest = request;

                AiProvider.OnProgress onProgress = status -> logger.debug("WorkerJob", "OnProgress: " + status.getWaitTime());
                AiProvider.OnResponse<GenerationDetailWithBitmap> onResponse = response -> {
                    logger.debug("WorkerJob", "Finished");
                    logger.debug("WorkerJob", "Model: " + response.getDetail().getModel());
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
                    try {
                        if (preferences.contains("storeWallpapersUri")) {
                            ContentResolverHelper.storeBitmap(getApplicationContext(), Uri.parse(preferences.getString("storeWallpapersUri", "")), UUID.randomUUID() + ".png", response.getImage());
                        }

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
                        logger.error("AIWallpaperError", "Failed setting new wallpaper", e);
                        completer.setException(e);
                    }
                };
                AtomicInteger censoredRetries = new AtomicInteger(3);
                ValueWrapper<AiHorde.OnError> onError = new ValueWrapper<>();
                onError.value = error -> {
                    if (error.getCause() instanceof RetryGenerationException) {
                        logger.debug("WorkerJob", "A recoverable error was caught, trying again", error.getCause());
                        aiHorde.generateImage(finalRequest, onProgress, onResponse, onError.value);
                        return;
                    }
                    if (error.getCause() instanceof ContentCensoredException && censoredRetries.get() > 0) {
                        logger.debug("HordeError", "Request got censored, retrying, remaining tries: " + censoredRetries.get());
                        censoredRetries.addAndGet(-1);
                        aiHorde.generateImage(finalRequest, onProgress, onResponse, onError.value);
                        return;
                    }

                    logger.error("AIWallpaperError", "Failed generating AI image", error);
                    if (error.networkResponse != null) {
                        logger.debug("AIWallpaperError", new String(error.networkResponse.data));
                    } else {
                        logger.debug("AIWallpaperError", error.getMessage(), error.getCause());
                    }
                    completer.setException(error);
                };

                aiHorde.generateImage(finalRequest, onProgress, onResponse, onError.value);
            });

            return "BillingManagerEnqueued";
        });
    }
}
