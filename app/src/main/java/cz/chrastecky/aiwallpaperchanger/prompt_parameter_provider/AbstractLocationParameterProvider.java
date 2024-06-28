package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.dto.LatitudeLongitude;
import cz.chrastecky.aiwallpaperchanger.exception.FailedGettingLocationException;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.aiwallpaperchanger.helper.SharedPreferencesHelper;
import cz.chrastecky.aiwallpaperchanger.helper.Tuple;

public abstract class AbstractLocationParameterProvider implements PromptParameterProvider {
    private static final Map<String, Tuple<String, Date>> cache = new HashMap<>();

    abstract protected void completeValue(@NonNull CompletableFuture<String> future, @NonNull final Context context, @NonNull LatitudeLongitude coordinates, @NonNull String parameterName);

    @Nullable
    @Override
    public CompletableFuture<String> getValue(@NonNull Context context, @NonNull String parameterName) {
        final Logger logger = new Logger(context);
        final CompletableFuture<String> future = new CompletableFuture<>();

        final Date now = new Date();
        @Nullable Tuple<String, Date> cacheItem = cache.getOrDefault(parameterName, null);
        if (cacheItem != null && now.before(cacheItem.value2)) {
            future.complete(cacheItem.value1);
        } else {
            new Thread(() -> {
                FusedLocationProviderClient locationProviderClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
                try {
                    locationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
                        double latitude;
                        double longitude;

                        SharedPreferences preferences = new SharedPreferencesHelper().get(context);
                        if (location == null) {
                            if (preferences.contains(SharedPreferencesHelper.LAST_KNOWN_LOCATION)) {
                                logger.debug("LocationParameter", "Failed getting location, using cached version.");
                                List<String> oldLocationRaw = new ArrayList<>(preferences.getStringSet(SharedPreferencesHelper.LAST_KNOWN_LOCATION, new HashSet<>(Arrays.asList("0", "0"))));
                                latitude = Double.parseDouble(oldLocationRaw.get(0));
                                longitude = Double.parseDouble(oldLocationRaw.get(1));
                            } else {
                                future.completeExceptionally(new FailedGettingLocationException());
                                logger.error("LocationParameter", "Failed getting location");
                                return;
                            }
                        } else {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putStringSet(SharedPreferencesHelper.LAST_KNOWN_LOCATION, new HashSet<>(Arrays.asList(String.valueOf(latitude), String.valueOf(longitude))));
                            editor.apply();
                        }

                        LatitudeLongitude result = new LatitudeLongitude(latitude, longitude);

                        completeValue(future, context, result, parameterName);
                    });
                } catch (SecurityException e) {
                    future.completeExceptionally(e);
                }
            }).start();
        }

        return future;
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull List<String> grantedPermissions, @NonNull String parameterName) {
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
    public boolean permissionsSatisfied(@NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        boolean result = grantedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                || grantedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            result = result && grantedPermissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        return result;
    }

    protected void setCache(@NonNull String value, @NonNull String parameterName) {
        setCache(value, new Date(new Date().getTime() + 300_000), parameterName);
    }

    protected void setCache(@NonNull String value, @NonNull Date validUntil, @NonNull String parameterName) {
        cache.put(parameterName, new Tuple<>(value, validUntil));
    }
}
