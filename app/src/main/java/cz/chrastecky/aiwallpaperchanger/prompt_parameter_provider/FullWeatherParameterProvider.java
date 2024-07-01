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
import cz.chrastecky.aiwallpaperchanger.exception.InvalidWeatherResponse;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@InjectedPromptParameterProvider
public class FullWeatherParameterProvider extends AbstractLocationParameterProvider {
    @Override
    protected void completeValue(@NonNull CompletableFuture<String> future, @NonNull Context context, @NonNull LatitudeLongitude coordinates, @NonNull String parameterName) {
        new Thread(() -> {
            final Logger logger = new Logger(context);
            final OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                    .url("https://api.openweathermap.org/data/2.5/weather?lat=" + coordinates.getLatitude() + "&lon=" + coordinates.getLongitude() + "&appid=" + BuildConfig.WEATHER_API_KEY)
                    .build();

            try (final Response response = client.newCall(request).execute()) {
                final WeatherResponse result = new Gson().fromJson(
                        response.body().string(),
                        WeatherResponse.class
                );
                if (result.getWeather().isEmpty()) {
                    future.completeExceptionally(new InvalidWeatherResponse());
                    return;
                }

                final int code = result.getWeather().get(0).getId();
                switch (code) {
                    case 200:
                        future.complete("thunderstorm with light rain");
                        break;
                    case 201:
                        future.complete("thunderstorm with rain");
                        break;
                    case 202:
                        future.complete("thunderstorm with heavy rain");
                        break;
                    case 210:
                        future.complete("light thunderstorm");
                        break;
                    case 211:
                        future.complete("thunderstorm");
                        break;
                    case 212:
                        future.complete("heavy thunderstorm");
                        break;
                    case 221:
                        future.complete("ragged thunderstorm");
                        break;
                    case 230:
                        future.complete("thunderstorm with light drizzle");
                        break;
                    case 231:
                        future.complete("thunderstorm with drizzle");
                        break;
                    case 232:
                        future.complete("thunderstorm with heavy drizzle");
                        break;
                    default:
                        logger.debug("FullWeather", "Unknown weather code: " + code);
                        future.complete("unknown");
                }

                setCache(future.join(), parameterName);
            } catch (IOException | NullPointerException e) {
                future.completeExceptionally(e);
            }
        }).start();
    }

    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("full_weather"));
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_full_weather_description);
    }
}
