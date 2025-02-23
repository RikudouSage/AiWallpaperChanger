package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location as SunCalcLocation;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class TimeOfDayParameterProvider implements PromptParameterProvider {
    private final FusedLocationProviderClient locationClient;
    private final SharedPreferences prefs;
    private boolean locationFetched = false;
    private double latitude;
    private double longitude;

    public TimeOfDayParameterProvider(Context context) {
        prefs = context.getSharedPreferences("TimeOfDayPrefs", Context.MODE_PRIVATE);
        locationClient = LocationServices.getFusedLocationProviderClient(context);
        loadCachedData();
        requestLocationUpdates(context);
    }

    private void loadCachedData() {
        latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToLongBits(0.0)));
        longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToLongBits(0.0)));
        locationFetched = prefs.getBoolean("locationFetched", false);
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates(Context context) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != 
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return; // Ensure permissions are granted before requesting location
        }

        LocationRequest locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(86400000) // Update location once per day (24 hours)
            .setFastestInterval(43200000); // Minimum update every 12 hours

        locationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    Location loc = locationResult.getLastLocation();
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                    locationFetched = true;
                    saveCachedData();
                }
            }
        }, Looper.getMainLooper());
    }

    private void saveCachedData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("latitude", Double.doubleToLongBits(latitude));
        editor.putLong("longitude", Double.doubleToLongBits(longitude));
        editor.putBoolean("locationFetched", locationFetched);
        editor.apply();
    }

    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Collections.singletonList("tod"));
    }

    @NonNull
    @Override
    public CompletableFuture<String> getValue(@NonNull final Context context, @NonNull String parameterName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!locationFetched) {
                return "unknown"; // If location isn't available yet
            }

            // Check if cached sunrise/sunset data is available and valid
            long lastUpdate = prefs.getLong("lastSunCalcUpdate", 0);
            if (System.currentTimeMillis() - lastUpdate < 86400000) { // Cached data is still valid for today
                return prefs.getString("lastTimeOfDay", "unknown");
            }

            // Fetch fresh sunrise/sunset times
            SunCalcLocation sunLocation = new SunCalcLocation(String.valueOf(latitude), String.valueOf(longitude));
            SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(sunLocation, TimeZone.getDefault());

            Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(Calendar.getInstance());
            Calendar sunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());

            int sunriseHour = sunrise.get(Calendar.HOUR_OF_DAY);
            int sunsetHour = sunset.get(Calendar.HOUR_OF_DAY);
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            String result;
            if (hour == 0) {
                result = "midnight";
            } else if (hour >= sunsetHour + 2 || hour <= sunriseHour - 2) {
                result = "night";
            } else if (hour >= sunriseHour - 2 && hour < sunriseHour) {
                result = "dawn";
            } else if (hour >= sunriseHour && hour < 12) {
                result = "morning";
            } else if (hour == 12) {
                result = "noon";
            } else if (hour > 12 && hour < sunsetHour - 2) {
                result = "afternoon";
            } else if (hour >= sunsetHour - 2 && hour < sunsetHour) {
                result = "evening";
            } else {
                result = "dusk";
            }

            // Cache the new result
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("lastTimeOfDay", result);
            editor.putLong("lastSunCalcUpdate", System.currentTimeMillis());
            editor.apply();

            return result;
        });
    }

    @NonNull
    @Override
    public String getDescription(@NonNull final Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_time_of_day_description);
    }

    @Nullable
    @Override
    public List<String> getRequiredPermissions(@NonNull Context context, @NonNull List<String> grantedPermissions, @NonNull String parameterName) {
        return null;
    }
}
