package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.LatitudeLongitude;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class CountryParameterProvider extends AbstractLocationParameterProvider {
    @Override
    protected void completeValue(@NonNull CompletableFuture<String> future, @NonNull Context context, @NonNull LatitudeLongitude coordinates, @NonNull String parameterName) {
        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(coordinates.getLatitude(), coordinates.getLongitude(), 1);
            if (addresses != null) {
                future.complete(addresses.get(0).getCountryName());
            } else {
                future.completeExceptionally(new RuntimeException("Failed getting address from location"));
            }
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
    }

    @NonNull
    @Override
    public List<String> getParameterNames() {
        return Collections.singletonList("country");
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        return context.getString(R.string.app_parameter_country_description);
    }
}
