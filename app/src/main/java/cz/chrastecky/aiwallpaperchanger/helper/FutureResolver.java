package cz.chrastecky.aiwallpaperchanger.helper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FutureResolver {
    @Nullable
    public static <T> List<T> resolveFutures(@NonNull final List<Future<T>> futures, @NonNull final Logger logger) {
        final int size = futures.size();
        final List<T> result = new ArrayList<>(Collections.nCopies(size, null));
        final CountDownLatch latch = new CountDownLatch(size);

        for (int i = 0; i < size; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    T value = futures.get(index).get();
                    result.set(index, value);
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("FutureResolver", "Failed to resolve a future", e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("FutureResolver", "Failed waiting for futures", e);
            return null;
        }
        return result;
    }
}
