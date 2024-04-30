package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;

public class BillingHelper {
    public interface OnBillingClientInitialized {
        void onInitialized(BillingClient billingClient);
    }

    public interface OnBillingClientUnavailable {
        void onUnavailable();
    }

    public static void getBillingClient(
            Context context,
            OnBillingClientInitialized onInitialized,
            OnBillingClientUnavailable onUnavailable
    ) {
        if (!BuildConfig.BILLING_ENABLED) {
            return;
        }

        BillingClient billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, list) -> {

                })
                .enablePendingPurchases()
                .build();

        ValueWrapper<BillingClientStateListener> startConnectionListener = new ValueWrapper<>();
        startConnectionListener.value = new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                billingClient.startConnection(startConnectionListener.value);
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    onInitialized.onInitialized(billingClient);
                } else {
                    onUnavailable.onUnavailable();
                }
            }
        };
        billingClient.startConnection(startConnectionListener.value);
    }
}
