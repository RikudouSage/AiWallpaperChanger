package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.LatitudeLongitude;
import cz.chrastecky.aiwallpaperchanger.dto.response.weather.WeatherResponse;
import cz.chrastecky.aiwallpaperchanger.exception.InvalidWeatherResponse;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.ThreadHelper;
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
            ThreadHelper.setupErrorHandler(logger);

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
                        logger.error("FullWeather", "Invalid weather response received");
                        future.complete("");
                        return;
                    }

                final Map<Integer, String> weatherMap = new HashMap<Integer, String>() {{
                    put(200, "thunderstorm with light rain");
                    put(201, "thunderstorm with rain");
                    put(202, "thunderstorm with heavy rain");
                    put(210, "light thunderstorm");
                    put(211, "thunderstorm");
                    put(212, "heavy thunderstorm");
                    put(221, "ragged thunderstorm");
                    put(230, "thunderstorm with light drizzle");
                    put(231, "thunderstorm with drizzle");
                    put(232, "thunderstorm with heavy drizzle");
                    put(300, "light intensity drizzle");
                    put(301, "drizzle");
                    put(302, "heavy intensity drizzle");
                    put(310, "light intensity drizzle rain");
                    put(311, "drizzle rain");
                    put(312, "heavy intensity drizzle rain");
                    put(313, "shower rain and drizzle");
                    put(314, "heavy shower rain and drizzle");
                    put(321, "shower drizzle");
                    put(500, "light rain");
                    put(501, "moderate rain");
                    put(502, "heavy intensity rain");
                    put(503, "very heavy rain");
                    put(504, "extreme rain");
                    put(511, "freezing rain");
                    put(520, "light intensity shower rain");
                    put(521, "shower rain");
                    put(522, "heavy intensity shower rain");
                    put(531, "ragged shower rain");
                    put(600, "light snow");
                    put(601, "snow");
                    put(602, "heavy snow");
                    put(611, "sleet");
                    put(612, "light shower sleet");
                    put(613, "shower sleet");
                    put(615, "light rain and snow");
                    put(616, "rain and snow");
                    put(620, "light shower snow");
                    put(621, "shower snow");
                    put(622, "heavy shower snow");
                    put(701, "mist");
                    put(711, "smoke");
                    put(721, "haze");
                    put(731, "sand/dust whirls");
                    put(741, "fog");
                    put(751, "sand");
                    put(761, "dust");
                    put(762, "volcanic ash");
                    put(771, "squalls");
                    put(781, "tornado");
                    put(800, "clear sky");
                    put(801, "few clouds");
                    put(802, "scattered clouds");
                    put(803, "broken clouds");
                    put(804, "overcast clouds");
                }};

                final int code = result.getWeather().get(0).getId();
                future.complete(weatherMap.getOrDefault(code, "unknown"));
                if (future.join().equals("unknown")) {
                    logger.debug("FullWeather", "Unknown weather code: " + code);
                }

                setCache(future.join(), parameterName);
            } catch (IOException | NullPointerException e) {
                logger.error("FullWeather", "Got an Exception when getting weather", e);
                future.complete("");
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
