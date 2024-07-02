package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.LatitudeLongitude;
import cz.chrastecky.aiwallpaperchanger.dto.response.weather.WeatherResponse;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@InjectedPromptParameterProvider
public class WeatherParameterProvider extends AbstractLocationParameterProvider {
    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("weather"));
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_weather_description);
    }

    @Override
    protected void completeValue(@NonNull CompletableFuture<String> future, @NonNull Context context, @NonNull LatitudeLongitude coordinates, @NonNull String parameterName) {
        ThreadHelper.runInThread(() -> {
            final Logger logger = new Logger(context);
            ThreadHelper.setupErrorHandler(logger);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.openweathermap.org/data/2.5/weather?lat=" + coordinates.getLatitude() + "&lon=" + coordinates.getLongitude() + "&appid=" + BuildConfig.WEATHER_API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                WeatherResponse result = new Gson().fromJson(
                        response.body().string(),
                        WeatherResponse.class
                );
                if (result.getWeather().isEmpty()) {
                    logger.error("Weather", "Invalid weather response received");
                    future.complete("");
                    return;
                }

                final int code = result.getWeather().get(0).getId();
                if (code >= 200 && code < 300) {
                    future.complete("stormy");
                } else if (code >= 300 && code < 500) {
                    future.complete("drizzly");
                } else if (code >= 500 && code < 600) {
                    future.complete("rainy");
                } else if (code >= 600 && code < 700) {
                    future.complete("snowy");
                } else if (code == 701) {
                    future.complete("misty");
                } else if (code == 711) {
                    future.complete("smokey");
                } else if (code == 721) {
                    future.complete("hazey");
                } else if (code == 731 || code == 761) {
                    future.complete("dusty");
                } else if (code == 741) {
                    future.complete("foggy");
                } else if (code == 751) {
                    future.complete("sandy");
                } else if (code == 762) {
                    future.complete("ashen");
                } else if (code == 800 || code == 801) {
                    future.complete("clear");
                } else if (code > 800) {
                    future.complete("cloudy");
                } else {
                    future.complete("unknown");
                }

                setCache(future.join(), parameterName);
            } catch (IOException | NullPointerException e) {
                logger.error("FullWeather", "Got an Exception when getting weather", e);
                future.complete("");
            }
        }, context);
    }
}
