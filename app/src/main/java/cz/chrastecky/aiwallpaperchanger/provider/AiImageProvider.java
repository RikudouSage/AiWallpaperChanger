package cz.chrastecky.aiwallpaperchanger.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.VolleyError;

import java.util.List;

import cz.chrastecky.aiwallpaperchanger.dto.GenerateRequest;
import cz.chrastecky.aiwallpaperchanger.dto.response.ActiveModel;
import cz.chrastecky.aiwallpaperchanger.dto.response.AsyncRequestStatusCheck;
import cz.chrastecky.aiwallpaperchanger.dto.response.GenerationDetailWithBitmap;
import cz.chrastecky.aiwallpaperchanger.helper.CancellationToken;

public interface AiImageProvider {
    interface OnResponse<T> {
        void onResponse(T response);
    }
    interface OnError {
        void onError(VolleyError error);
    }
    interface OnProgress {
        void onProgress(AsyncRequestStatusCheck status);
    }

    void getModels(@NonNull OnResponse<List<ActiveModel>> onResponse, @Nullable OnError onError);
    void generateImage(@NonNull GenerateRequest request, @NonNull OnProgress onProgress, @NonNull OnResponse<GenerationDetailWithBitmap> onResponse, @Nullable OnError onError, @Nullable CancellationToken cancellationToken);
}
