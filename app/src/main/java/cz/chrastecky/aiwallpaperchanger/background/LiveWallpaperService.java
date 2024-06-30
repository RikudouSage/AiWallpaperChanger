package cz.chrastecky.aiwallpaperchanger.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

import cz.chrastecky.aiwallpaperchanger.action.LiveWallpaperAction;
import cz.chrastecky.aiwallpaperchanger.helper.Logger;

public class LiveWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new LiveWallpaperEngine();
    }

    private class LiveWallpaperEngine extends Engine {
        private final Logger logger = new Logger(getApplicationContext());
        private BroadcastReceiver receiver;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

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

        private void updateWallpaper() {
            File imageFile = new File(getFilesDir(), "currentImage.webp");
            if (!imageFile.exists()) {
                return;
            }

            final Bitmap image = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            updateWallpaper(image);
        }

        private void updateWallpaper(Bitmap image) {
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

            while (targetWidth / width > 1 || targetHeight / height > 1) {
                targetWidth /= 2;
                targetHeight /= 2;
            }

            if (targetWidth < width || targetHeight < height) {
                double coefficientWidth = (double) targetWidth / width;
                double coefficientHeight = (double) targetHeight / height;

                if (coefficientWidth < coefficientHeight) {
                    targetWidth = width;
                    targetHeight = (int) (targetHeight * coefficientWidth);
                } else {
                    targetWidth = (int) (targetWidth * coefficientHeight);
                    targetHeight = height;
                }
            }

            canvas.drawBitmap(image, null, new RectF(0, 0, targetWidth, targetHeight), null);
            holder.unlockCanvasAndPost(canvas);
        }
    }
}