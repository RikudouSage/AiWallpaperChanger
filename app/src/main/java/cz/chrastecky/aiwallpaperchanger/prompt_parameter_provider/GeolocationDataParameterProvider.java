package cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.dto.LatitudeLongitude;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;
import cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider;

@InjectedPromptParameterProvider
public class GeolocationDataParameterProvider extends AbstractLocationParameterProvider {
    private final static String PARAMETER_COUNTRY = "country";
    private final static String PARAMETER_TOWN = "town";
    private final static String PARAMETER_STATE = "state";

    @Override
    protected void completeValue(@NonNull CompletableFuture<String> future, @NonNull Context context, @NonNull LatitudeLongitude coordinates, @NonNull String parameterName) {
        final Logger logger = new Logger(context);
        Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);
        try {
            List<Address> addresses = geocoder.getFromLocation(coordinates.getLatitude(), coordinates.getLongitude(), 10);
            if (addresses != null && !addresses.isEmpty()) {
                Optional<Address> target;
                switch (parameterName) {
                    case PARAMETER_COUNTRY:
                        target = addresses.stream().filter(address -> address.getCountryName() != null).findFirst();
                        if (!target.isPresent()) {
                            logger.error("GeolocationData", "No address with valid country found");
                            future.complete("");
                            return;
                        }
                        future.complete(target.get().getCountryName());
                        break;
                    case PARAMETER_TOWN:
                        target = addresses.stream().filter(address -> address.getLocality() != null).findFirst();
                        if (!target.isPresent()) {
                            logger.error("GeolocationData", "No address with valid locality found");
                            future.complete("");
                            return;
                        }
                        future.complete(target.get().getLocality());
                        break;
                    case PARAMETER_STATE:
                        target = addresses.stream().filter(address -> address.getAdminArea() != null).findFirst();
                        if (!target.isPresent()) {
                            logger.error("GeolocationData", "No address with valid admin area found");
                            future.complete("");
                            return;
                        }
                        future.complete(target.get().getAdminArea());
                        break;
                    default:
                        logger.error("GeolocationData", "Unknown parameter somehow slipped through: " + parameterName);
                        future.complete("");
                        break;
                }
            } else {
                if (addresses == null) {
                    logger.error("GeolocationData", "Failed getting addresses (null)");
                } else {
                    logger.error("GeolocationData", "Failed getting addresses (empty list)");
                }
                future.complete("");
            }
        } catch (IOException e) {
            logger.error("GeolocationData", "Caught IOException", e);
            future.complete("");
        }
    }

    @NonNull
    @Override
    public CompletableFuture<List<String>> getParameterNames(@NonNull Context context) {
        return CompletableFuture.completedFuture(Arrays.asList(PARAMETER_COUNTRY, PARAMETER_TOWN, PARAMETER_STATE));
    }

    @NonNull
    @Override
    public String getDescription(@NonNull Context context, @NonNull String parameterName) {
        switch (parameterName) {
            case PARAMETER_COUNTRY:
                return context.getString(R.string.app_parameter_country_description);
            case PARAMETER_TOWN:
                return context.getString(R.string.app_parameter_town_description);
            case PARAMETER_STATE:
                return context.getString(R.string.app_parameter_state_description);
        }

        new Logger(context).error("GeolocationParameter", "Unsupported parameter somehow got returned: " + parameterName);
        return "";
    }
}
