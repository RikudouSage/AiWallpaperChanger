package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.response.weather.WeatherResponse;
import cz.chrastecky.aiwallpaperchanger.exception.InvalidWeatherResponse;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@InjectedPromptParameterProvider
public class WeatherParameterProvider implements PromptParameterProvider {
    @Nullable private static String cached = null;
    @Nullable private static Date validUntil = null;

    @NonNull
    @Override
    public String getParameterName() {
        return "weather";
    }

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull Context context) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        final Date now = new Date();
        if (cached != null && validUntil != null && now.before(validUntil)) {
            future.complete(cached);
        } else {
            new Thread(() -> {
                FusedLocationProviderClient locationProviderClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
                try {
                    locationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
                        new Thread(() -> {
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://api.openweathermap.org/data/2.5/weather?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&appid=" + BuildConfig.WEATHER_API_KEY)
                                    .build();

                            try (Response response = client.newCall(request).execute()) {
                                WeatherResponse result = new Gson().fromJson(
                                        response.body().string(),
                                        WeatherResponse.class
                                );
                                if (result.getWeather().isEmpty()) {
                                    future.completeExceptionally(new InvalidWeatherResponse());
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

                                cached = future.join();
                                validUntil = new Date(now.getTime() + 300_000);
                            } catch (IOException | NullPointerException e) {
                                future.completeExceptionally(e);
                            }
                        }).start();
                    });
                } catch (SecurityException e) {
                    future.completeExceptionally(e);
                }
            }).start();
        }

        return future;
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context) {
        return context.getString(R.string.app_parameter_weather_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull List<String> grantedPermissions) {
        final List<String> result = new ArrayList<>(Arrays.asList(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        ));
        if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && (
                    grantedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                    || grantedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)
                )
        ) {
            result.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        return result;
    }

    @Override
    public boolean permissionsSatisfied(@NonNull List<String> grantedPermissions) {
        boolean result = grantedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                || grantedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result = result && grantedPermissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        return result;
    }
}
