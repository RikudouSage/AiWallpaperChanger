package cz.chrastecky.aiwallpaperchanger.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import cz.chrastecky.aiwallpaperchanger.BuildConfig;
import cz.chrastecky.aiwallpaperchanger.R;
import cz.chrastecky.aiwallpaperchanger.databinding.ActivityPremiumBinding;
import cz.chrastecky.aiwallpaperchanger.helper.BillingHelper;

public class PremiumActivity extends AppCompatActivity {
    private final static String PREMIUM_PURCHASE_NAME = "premium_api_key";

    private ProductDetails productDetails;
    @Nullable
    private BillingClient billingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BuildConfig.BILLING_ENABLED) {
            finish();
            return;
        }

        ActivityPremiumBinding binding = ActivityPremiumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        setTitle(R.string.app_title_premium);

        Button activatePremiumButton = findViewById(R.id.activate_premium_button);
        activatePremiumButton.setOnClickListener(view -> {
            if (billingClient == null) {
                Toast.makeText(this, R.string.app_premium_billing_unavailable, Toast.LENGTH_LONG).show();
                return;
            }
            ProgressBar loader = findViewById(R.id.purchase_loader);
            loader.setVisibility(View.VISIBLE);
            view.setVisibility(View.INVISIBLE);

            BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
                    .build();
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(new ArrayList<>(Collections.singleton(productDetailsParams)))
                    .build();
            BillingResult result = billingClient.launchBillingFlow(this, billingFlowParams);
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Toast.makeText(this, R.string.app_premium_error_launching_subscribe, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (billingClient == null) {
            initializeBilling();
        } else {
            loadPurchases();
        }
    }

    private void loadPurchases() {
        ProgressBar loader = findViewById(R.id.purchase_loader);
        TextView unavailable = findViewById(R.id.billing_unavailable);
        TextView purchased = findViewById(R.id.already_purchased);
        Button activatePremium = findViewById(R.id.activate_premium_button);

        loader.setVisibility(View.VISIBLE);
        unavailable.setVisibility(View.INVISIBLE);
        activatePremium.setVisibility(View.INVISIBLE);
        purchased.setVisibility(View.INVISIBLE);

        assert billingClient != null;
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(), (billingResult, list) -> {
            List<List<String>> purchasedIds = list.stream()
                    .map(purchase -> purchase.getProducts())
                    .filter(purchases -> purchases.contains(PREMIUM_PURCHASE_NAME))
                    .collect(Collectors.toList());

            if (purchasedIds.isEmpty()) {
                loader.setVisibility(View.INVISIBLE);
                unavailable.setVisibility(View.INVISIBLE);
                purchased.setVisibility(View.INVISIBLE);
                activatePremium.setVisibility(View.VISIBLE);
                return;
            }

            loader.setVisibility(View.INVISIBLE);
            activatePremium.setVisibility(View.INVISIBLE);
            unavailable.setVisibility(View.INVISIBLE);
            purchased.setVisibility(View.VISIBLE);
        });
    }

    private void initializeBilling() {
        ProgressBar loader = findViewById(R.id.purchase_loader);
        TextView unavailable = findViewById(R.id.billing_unavailable);
        Button activatePremium = findViewById(R.id.activate_premium_button);

        loader.setVisibility(View.VISIBLE);
        unavailable.setVisibility(View.INVISIBLE);
        activatePremium.setVisibility(View.INVISIBLE);

        BillingHelper.getBillingClient(this, billingClient -> {
            this.billingClient = billingClient;
            QueryProductDetailsParams productDetailsQuery = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                            new ArrayList<>(
                                    Collections.singleton(QueryProductDetailsParams.Product.newBuilder().setProductType(BillingClient.ProductType.SUBS).setProductId("premium_api_key").build())
                            )
                    )
                    .build();
            billingClient.queryProductDetailsAsync(productDetailsQuery, (billingResult, list) -> {
                if (list.isEmpty()) {
                    Toast.makeText(this, R.string.app_premium_unavailable, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                this.productDetails = list.get(0);
                loadPurchases();
            });
        }, this::onBillingUnavailable);
    }

    private void onBillingUnavailable() {
        ProgressBar loader = findViewById(R.id.purchase_loader);
        TextView unavailable = findViewById(R.id.billing_unavailable);
        TextView purchased = findViewById(R.id.already_purchased);
        Button activatePremium = findViewById(R.id.activate_premium_button);

        loader.setVisibility(View.INVISIBLE);
        unavailable.setVisibility(View.VISIBLE);
        purchased.setVisibility(View.INVISIBLE);
        activatePremium.setVisibility(View.INVISIBLE);
    }
}