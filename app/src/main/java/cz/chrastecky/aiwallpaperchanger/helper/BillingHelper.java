package cz.chrastecky.aiwallpaperchanger.helper;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.List;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;

public class BillingHelper {
    public interface OnBillingClientInitialized {
        void onInitialized(BillingClient billingClient);
    }

    public interface OnBillingClientUnavailable {
        void onUnavailable();
    }

    public interface OnPurchaseStatusResolved {
        void onPurchaseStatusResolved(boolean status);
    }

    public static void getBillingClient(
            Context context,
            OnBillingClientInitialized onInitialized,
            OnBillingClientUnavailable onUnavailable
    ) {
        if (!BuildConfig.BILLING_ENABLED) {
            onUnavailable.onUnavailable();
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

    public static void getPurchaseStatus(Context context, String purchaseName, OnPurchaseStatusResolved onPurchaseStatusResolved) {
        getBillingClient(context, billingClient -> {
            billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), (billingResult, list) -> {
                List<Purchase> purchasedIds = list.stream()
                        .filter(purchase -> purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED)
                        .filter(purchase -> purchase.getProducts().contains(purchaseName))
                        .collect(Collectors.toList());
                onPurchaseStatusResolved.onPurchaseStatusResolved(!purchasedIds.isEmpty());
            });
        }, () -> onPurchaseStatusResolved.onPurchaseStatusResolved(false));
    }
}
