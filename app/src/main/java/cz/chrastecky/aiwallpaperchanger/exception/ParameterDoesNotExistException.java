package cz.chrastecky.aiwallpaperchanger.exception;

import androidx.annotation.NonNull;

public class ParameterDoesNotExistException extends RuntimeException {
    public ParameterDoesNotExistException(@NonNull String parameterName) {
        super("The parameter name does not exist: " + parameterName);
    }
}
