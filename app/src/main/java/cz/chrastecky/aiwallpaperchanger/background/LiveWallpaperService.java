package cz.chrastecky.aiwallpaperchanger.background;

import android.app.WallpaperColors;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

import cz.chrastecky.aiwallpaperchanger.action.LiveWallpaperAction;
import cz.chrastecky.aiwallpaperchanger.helper.CurrentWallpaperHelper;

public class LiveWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new LiveWallpaperEngine();
    }

    private class LiveWallpaperEngine extends Engine {
        private BroadcastReceiver receiver;
        private Bitmap currentBitmap;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            setTouchEventsEnabled(false);

            if (!isPreview()) {
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction() == null || !intent.getAction().equals(LiveWallpaperAction.INTENT_ACTION)) {
                            return;
                        }

                        updateWallpaper();
                    }
                };
                IntentFilter intentFilter = new IntentFilter(LiveWallpaperAction.INTENT_ACTION);
                LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, intentFilter);
            }
        }

        @Override
        public void onDestroy() {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
            super.onDestroy();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            updateWallpaper();
        }

        @Nullable
        @Override
        public WallpaperColors onComputeColors() {
            if (currentBitmap == null) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                return WallpaperColors.fromBitmap(currentBitmap);
            }

            return null;
        }

        private void updateWallpaper() {
            final Bitmap currentImage = CurrentWallpaperHelper.getBitmap(LiveWallpaperService.this);
            if (currentImage == null) {
                return;
            }

            updateWallpaper(currentImage);
        }

        private void updateWallpaper(Bitmap image) {
            currentBitmap = image;
            SurfaceHolder holder = getSurfaceHolder();

            Canvas canvas = holder.lockCanvas();
            if (canvas == null) {
                return;
            }

            DisplayMetrics metrics = new DisplayMetrics();

            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);

            final int width = metrics.widthPixels;
            final int height = metrics.heightPixels;

            int targetWidth = image.getWidth();
            int targetHeight = image.getHeight();

            if ((double) width / targetWidth != 1 || (double) height / targetHeight != 1) {
                double coefficientWidth = (double) width / targetWidth;
                double coefficientHeight = (double) height / targetHeight;

                double coefficient = Math.max(coefficientWidth, coefficientHeight);

                targetWidth = (int) (targetWidth * coefficient);
                targetHeight = (int) (targetHeight * coefficient);
            }

            canvas.drawBitmap(image, null, new RectF(0, 0, targetWidth, targetHeight), null);
            holder.unlockCanvasAndPost(canvas);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                notifyColorsChanged();
            }
        }
    }
}