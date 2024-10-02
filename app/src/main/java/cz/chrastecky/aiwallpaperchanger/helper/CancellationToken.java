package cz.chrastecky.aiwallpaperchanger.helper;

public class CancellationToken {
    private boolean cancelled = false;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }
}
