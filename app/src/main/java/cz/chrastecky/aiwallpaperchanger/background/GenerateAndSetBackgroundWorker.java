package cz.chrastecky.aiwallpaperchanger.background;

import android.annotation.SuppressLint;
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
import cz.chrastecky.aiwallpaperchanger.action.StaticWallpaperAction;
import cz.chrastecky.aiwallpaperchanger.action.WallpaperAction;
import cz.chrastecky.aiwallpaperchanger.action.WallpaperActionCollection;
import cz.chrastecky.aiwallpaperchanger.activity.PremiumActivity;
import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.StoredRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.exception.ContentCensoredException;
import cz.chrastecky.aiwallpaperchanger.exception.RetryGenerationException;
import cz.chrastecky.aiwallpaperchanger.helper.ApiKeyHelper;
import cz.chrastecky.aiwallpaperchanger.helper.BillingHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ContentResolverHelper;
import cz.chrastecky.aiwallpaperchanger.helper.GenerateRequestHelper;
import cz.chrastecky.aiwallpaperchanger.helper.History;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.PromptReplacer;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.ValueWrapper;
import cz.chrastecky.aiwallpaperchanger.helper.WallpaperFileHelper;
import cz.chrastecky.aiwallpaperchanger.provider.AiHorde;
import cz.chrastecky.aiwallpaperchanger.provider.AiImageProvider;

public class GenerateAndSetBackgroundWorker extends ListenableWorker {
    private final Logger logger = new Logger(getApplicationContext());
    private final WallpaperActionCollection wallpaperActionCollection = new WallpaperActionCollection();

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
                    ApiKeyHelper.setDefaultApiKey(BuildConfig.PREMIUM_API_KEY);
                }

                logger.debug("WorkerJob", "Inside doWork()");
                AiHorde aiHorde = new AiHorde(getApplicationContext());
                SharedPreferences preferences = new SharedPreferencesHelper().get(getApplicationContext());

                if (!preferences.contains(SharedPreferencesHelper.STORED_GENERATION_PARAMETERS)) {
                    completer.set(Result.failure());
                    return;
                }

                String requestJson = preferences.getString(SharedPreferencesHelper.STORED_GENERATION_PARAMETERS, "");
                logger.debug("WorkerJob", "Request: " + requestJson);
                GenerateRequest request = GenerateRequestHelper.parse(preferences.getString(SharedPreferencesHelper.STORED_GENERATION_PARAMETERS, ""));
                PromptReplacer.replacePrompt(getApplicationContext(), request.getPrompt(), replaced -> {
                    if (replaced == null) {
                        logger.error("WorkerJob", "Failed replacing parameters");
                        return;
                    }
                    GenerateRequest newRequest = GenerateRequestHelper.withPrompt(request, replaced);

                    if (!BuildConfig.NSFW_ENABLED && newRequest.getNsfw()) {
                        newRequest = GenerateRequestHelper.disableNsfw(newRequest);
                    }
                    final GenerateRequest finalRequest = newRequest;

                    AiImageProvider.OnProgress onProgress = status -> logger.debug("WorkerJob", "OnProgress: " + status.getWaitTime());
                    AiImageProvider.OnResponse<GenerationDetailWithBitmap> onResponse = response -> {
                        logger.debug("WorkerJob", "Finished");
                        logger.debug("WorkerJob", "Model: " + response.getDetail().getModel());

                        try {
                            logger.debug("WorkerJob", "Trying to save current image");
                            WallpaperFileHelper.save(getApplicationContext(), response.getImage());
                            logger.debug("WorkerJob", "Successfully saved the current image");
                        } catch (IOException e) {
                            logger.error("WorkerJob", "Failed saving the current image", e);
                        }

                        WallpaperAction wallpaperAction = wallpaperActionCollection.findById(
                                preferences.getString(SharedPreferencesHelper.WALLPAPER_ACTION, StaticWallpaperAction.ID)
                        );
                        logger.debug("WorkerJob", "Wallpaper action: " + wallpaperAction.getClass().getName());
                        if (!wallpaperAction.setWallpaper(getApplicationContext(), response.getImage())) {
                            logger.error("AIWallpaperError", "Failed setting new wallpaper");
                            completer.setException(new RuntimeException("Failed setting new wallpaper"));
                            return;
                        }
                        logger.debug("WorkerJob", "Wallpaper action finished successfully");

                        if (preferences.contains(SharedPreferencesHelper.STORE_WALLPAPERS_URI)) {
                            logger.debug("WorkerJob", "Storing image on the filesystem");
                            ContentResolverHelper.storeBitmap(getApplicationContext(), Uri.parse(preferences.getString(SharedPreferencesHelper.STORE_WALLPAPERS_URI, "")), UUID.randomUUID() + ".png", response.getImage());
                        }

                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(SharedPreferencesHelper.WALLPAPER_LAST_CHANGED, DateFormat.getInstance().format(Calendar.getInstance().getTime()));
                        editor.commit();

                        logger.debug("WorkerJob", "Storing item in history.");
                        History history = new History(getApplicationContext());
                        history.addItem(new StoredRequest(
                                UUID.randomUUID(),
                                finalRequest,
                                response.getDetail().getSeed(),
                                response.getDetail().getWorkerId(),
                                response.getDetail().getWorkerName(),
                                new Date(),
                                response.getDetail().getModel()
                        ));
                        logger.debug("WorkerJob", "Successfully created a history entry");

                        completer.set(Result.success());
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

                        logger.error("AIWallpaperError", "Failed generating AI image (" + error.getClass() + ")", error);
                        if (error.networkResponse != null) {
                            logger.debug("AIWallpaperError", new String(error.networkResponse.data));
                        } else {
                            logger.debug("AIWallpaperError", error.getMessage(), error.getCause());
                        }
                        completer.setException(error);
                    };

                    aiHorde.generateImage(finalRequest, onProgress, onResponse, onError.value);
                });
            });

            return "BillingManagerEnqueued";
        });
    }
}
