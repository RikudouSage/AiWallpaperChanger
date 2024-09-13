package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.dto.LatitudeLongitude;
import cz.chrastecky.aiwallpaperchanger.dto.response.weather.WeatherResponse;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class AbstractTemperatureParameterProvider extends AbstractLocationParameterProvider {
    abstract protected double convertToTargetUnit(double kelvin);
    abstract protected String getUnit();

    @Override
    protected void completeValue(@NonNull CompletableFuture<String> future, @NonNull Context context, @NonNull LatitudeLongitude coordinates, @NonNull String parameterName) {
        ThreadHelper.runInThread(() -> {
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

                final double temperature = convertToTargetUnit(result.getMain().getTemp());
                final String unit = getUnit();

                if ((double) (int) temperature != temperature) {
                    future.complete(temperature + unit);
                } else {
                    future.complete((int) temperature + unit);
                }

            } catch (IOException | NullPointerException e) {
                logger.error("AbstractTemperatureParameterProvider", "Got an Exception when getting weather", e);
                future.complete("");
            }
        }, context);
    }
}
