package com.example.piceditor.ads.iap;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.example.piceditor.MainActivity;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.R;
import com.example.piceditor.ads.Prefs;
import com.example.piceditor.databinding.ActivityPremiumBinding;
import com.example.piceditor.splash.SplashActivity;
import com.google.common.collect.ImmutableList;
import com.universe.translate.ads.iap.IapConnector;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;


public class PremiumActivity extends FragmentActivity {
    private Prefs prefs;
    private ImageView imgBack;
    public static String LIFETIME = "pro_lifetime";
    public static String MONTHLY = "1_mon";
    public static String WEEKLY = "1_week";
    public static String MONTHLY_6 = "6_mon";
    public static String YEAR = "1_year";
    private IapConnector iapConnector;
    private BillingClient billingClient;
    List<ProductDetails> productDetailsList;
    private boolean isMonthly = true;
    boolean isTrial;

    private ActivityPremiumBinding binding;

    private int selected = 0;

    // Giá (micros) để tính per-week + SAVE% cho gói Yearly
    private long weeklyMicros = 0L;
    private long yearlyMicros = 0L;
    private String priceCurrency = null;
    // Free-trial framing: giá đã format + số ngày trial mỗi gói (0 = không có trial)
    private String weeklyPriceFmt = null, yearlyPriceFmt = null;
    private int weeklyTrialDays = 0, yearlyTrialDays = 0;

    // Carousel feature auto-scroll
    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;

    public void setStatusBar(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }
    public void FullScreencall() {
        View decorView2 = getWindow().getDecorView();
        decorView2.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        if(Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if(Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE;
            decorView.setSystemUiVisibility(uiOptions);
        }
        decorView2.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_premium);
        if(getIntent().hasExtra("free_trial")){
            isTrial = true;
        }else isTrial = false;
        FullScreencall();
//        setStatusBar(getResources().getColor(R.color.color_excel_dark));

        prefs = new Prefs(this);
        //initializeBillingClient();
        initView();
        Log.e("xxx2", "onCreate: " );

        initIAP();
    }

    private void initIAP() {
        try {
            productDetailsList = new ArrayList<>();
            billingClient = BillingClient.newBuilder(this)
                    .enablePendingPurchases()
                    .setListener(
                            (billingResult, list) -> {
                                if(billingResult.getResponseCode()==BillingClient.BillingResponseCode.OK && list !=null) {
                                    for (Purchase purchase: list){
                                        verifySubPurchase(purchase);
                                    }
                                }
                            }
                    ).build();

            //start the connection after initializing the billing client
            establishConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void establishConnection() {
        try {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        // The BillingClient is ready. You can query purchases here.
                        showAllSub();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    establishConnection();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SuppressLint("SetTextI18n")
    void showAllSub() {

        try {
            ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                    //Product 1
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(WEEKLY)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),

//                    QueryProductDetailsParams.Product.newBuilder()
//                            .setProductId(MONTHLY_6)
//                            .setProductType(BillingClient.ProductType.SUBS)
//                            .build()
//                    ,
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(YEAR)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()

            );

            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build();

            billingClient.queryProductDetailsAsync(
                    params,
                    (billingResult, prodDetailsList) -> {
                        // Process the result
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                            try {
                                productDetailsList.addAll(prodDetailsList);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        for ( ProductDetails entry : prodDetailsList) {
                                            Log.e("xxx", "onPricesUpdated: " + entry);
                                            String sku = entry.getProductId();
                                            try {
                                                List<ProductDetails.PricingPhase> phases = entry.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList();
                                                ProductDetails.PricingPhase recurring = phases.get(phases.size() - 1);
                                                String price = recurring.getFormattedPrice();
                                                long micros = recurring.getPriceAmountMicros();
                                                priceCurrency = recurring.getPriceCurrencyCode();
                                                if (sku.equalsIgnoreCase(WEEKLY)) {
                                                    binding.tvWeeklyPrice.setText(price);
                                                    weeklyMicros = micros;
                                                    weeklyPriceFmt = price;
                                                    weeklyTrialDays = trialDays(entry);
                                                } else if (sku.equalsIgnoreCase(YEAR)) {
                                                    binding.tvYearlyPrice.setText(price);
                                                    yearlyMicros = micros;
                                                    yearlyPriceFmt = price;
                                                    yearlyTrialDays = trialDays(entry);
                                                }
                                            } catch (Exception e) {
                                                Log.e("xxx", "showProducts: "+e.getMessage());
                                                e.printStackTrace();
                                            }
                                        }
                                        updateYearlySavings();
                                        updateTrialFraming();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }


                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void showAllInApp() {

        try {
            ImmutableList<QueryProductDetailsParams.Product> productList = ImmutableList.of(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(LIFETIME)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()

            );

            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build();

            billingClient.queryProductDetailsAsync(
                    params,
                    (billingResult, prodDetailsList) -> {
                        // Process the result
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                            try {
                                productDetailsList.addAll(prodDetailsList);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        for ( ProductDetails entry : prodDetailsList) {
                                            Log.e("xxx", "onPricesUpdated: " + entry);
                                            String sku = entry.getProductId();
                                            String price = entry.getOneTimePurchaseOfferDetails().getFormattedPrice();
                                            try {
                                                if (sku.equalsIgnoreCase(LIFETIME)) {
                                                    Log.d("xxx", "lifetime price: " + price);
                                                }
                                            } catch (Exception e) {
                                                Log.e("xxx", "showProducts: "+e.getMessage());
                                                e.printStackTrace();
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }


                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void launchPurchaseFlow(ProductDetails productDetails) {
        try {
            assert productDetails.getSubscriptionOfferDetails() != null;
            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                    ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(pickOfferToken(productDetails))
                                    .build()
                    );
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();

            billingClient.launchBillingFlow(this, billingFlowParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    void launchPurchaseFlowForInApp(ProductDetails productDetails) {
        try {
            ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                    ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                    );
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();

            billingClient.launchBillingFlow(this, billingFlowParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void verifySubPurchase(Purchase purchases) {

        try {
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchases.getPurchaseToken())
                    .build();

            billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    //use prefs to set premium
                    //  Toast.makeText(Subscriptions.this, "Subscription activated, Enjoy!", Toast.LENGTH_SHORT).show();
                    //Setting premium to 1
                    // 1 - premium
                    // 0 - no premium
                    prefs.setPremium(1);
                    restart();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void onResume() {
        super.onResume();
        try {
            if(billingClient != null){
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                                    verifySubPurchase(purchase);
                                }
                            }
                        }
                    }
            );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void restart() {
        Intent intent = new Intent(this, SplashActivity.class);
        this.startActivity(intent);
        this.finishAffinity();
    }

    private void initView() {

        selectYearly();
        setupCarousel();

        binding.tvRestore.setOnClickListener(v -> restorePurchases());

        binding.layoutYearly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeatherApplication.trackingEvent("select_plan", "plan", "yearly");
                selectYearly();
            }
        });

        binding.layoutWeekly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeatherApplication.trackingEvent("select_plan", "plan", "weekly");
                selectWeekly();
            }
        });


        binding.btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeatherApplication.trackingEvent("click_subscribe", "plan", selected == 0 ? "yearly" : "weekly");
                if(selected == 0){
                    try {
                        for (ProductDetails productDetail : productDetailsList){
                            if(productDetail.getProductId().equals(YEAR)){
                                launchPurchaseFlow(productDetail);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else {
                    try {
                        for (ProductDetails productDetail : productDetailsList){
                            if(productDetail.getProductId().equals(WEEKLY)){
                                launchPurchaseFlow(productDetail);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });






//        binding.viewBuy4.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                disableAllBorder();
//                enableBorder(binding.viewBorder4);
//                try {
//                    //iapConnector.subscribe(PremiumActivity.this, YEARLY);
//                    try {
//                        for (ProductDetails productDetail : productDetailsList){
//                            if(productDetail.getProductId().equals(LIFETIME)){
//                                launchPurchaseFlowForInApp(productDetail);
//                                return;
//                            }
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        binding.viewBuy1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                disableAllBorder();
//                enableBorder(binding.viewBorder1);
//                try {
//                    for (ProductDetails productDetail : productDetailsList){
//                        if(productDetail.getProductId().equals(MONTHLY)){
//                            launchPurchaseFlow(productDetail);
//                            return;
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        binding.viewBuy2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                disableAllBorder();
//                enableBorder(binding.viewBorder2);
//                try {
//                    for (ProductDetails productDetail : productDetailsList){
//                        if(productDetail.getProductId().equals(MONTHLY_6)){
//                            launchPurchaseFlow(productDetail);
//                            return;
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        binding.viewBuy3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                disableAllBorder();
//                enableBorder(binding.viewBorder3);
//                try {
//                    for (ProductDetails productDetail : productDetailsList){
//                        if(productDetail.getProductId().equals(YEAR)){
//                            launchPurchaseFlow(productDetail);
//                            return;
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });




        binding.imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


//        Glide.with(this)
//                .load(R.drawable.bg_premium)
//                .transform(new GlideBlurTransformation(this))
//                .into(binding.imgBackground);


    }

    @Override
    public void onBackPressed() {
        if(isTrial){
            startActivity(new Intent(PremiumActivity.this, MainActivity.class));
            finish();
        }else {
            super.onBackPressed();
        }

    }


    private void selectYearly() {
        binding.layoutYearly.setBackgroundResource(R.drawable.bg_iap_plan_selected);
        binding.layoutWeekly.setBackgroundResource(R.drawable.bg_iap_plan_unselected);
        binding.tvYearlyPrice.setTextColor(Color.parseColor("#B3FF10"));
        binding.tvWeeklyPrice.setTextColor(Color.WHITE);
        selected = 0;
        updateTrialFraming();
    }

    private void selectWeekly() {
        binding.layoutWeekly.setBackgroundResource(R.drawable.bg_iap_plan_selected);
        binding.layoutYearly.setBackgroundResource(R.drawable.bg_iap_plan_unselected);
        binding.tvWeeklyPrice.setTextColor(Color.parseColor("#B3FF10"));
        binding.tvYearlyPrice.setTextColor(Color.WHITE);
        selected = 1;
        updateTrialFraming();
    }

    // ĐÃ TẮT free-trial theo yêu cầu: luôn "mua thẳng" -> CTA "Subscribe Now" + "Cancel anytime",
    // KHÔNG bao giờ hiện framing "X-day free trial / Start Free Trial" dù Play Console có offer trial.
    @SuppressLint("SetTextI18n")
    private void updateTrialFraming() {
        try {
            binding.tvCancel.setText(getString(R.string.iap_cancel_anytime));
            binding.tvSubscribe.setText(getString(R.string.iap_subscribe_now));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Số ngày free-trial của 1 SKU (0 nếu không có offer trial). Tìm offer có phase giá = 0.
    private int trialDays(ProductDetails pd) {
        try {
            if (pd.getSubscriptionOfferDetails() == null) return 0;
            for (ProductDetails.SubscriptionOfferDetails offer : pd.getSubscriptionOfferDetails()) {
                for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                    if (phase.getPriceAmountMicros() == 0) {
                        return periodToDays(phase.getBillingPeriod());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ISO-8601 billing period (vd "P3D", "P1W", "P1M") -> số ngày.
    private int periodToDays(String iso) {
        try {
            if (iso == null) return 0;
            int days = 0;
            java.util.regex.Matcher mw = java.util.regex.Pattern.compile("(\\d+)W").matcher(iso);
            if (mw.find()) days += Integer.parseInt(mw.group(1)) * 7;
            java.util.regex.Matcher md = java.util.regex.Pattern.compile("(\\d+)D").matcher(iso);
            if (md.find()) days += Integer.parseInt(md.group(1));
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("(\\d+)M").matcher(iso);
            if (mm.find()) days += Integer.parseInt(mm.group(1)) * 30;
            return days;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Carousel feature (ViewPager2 + dots + auto-scroll) ──
    private void setupCarousel() {
        // Khớp thứ tự ảnh trong IapFeatureAdapter: Remove ads (feature_2) lên đầu
        String[] captions = {
                getString(R.string.iap_feature_2),
                getString(R.string.iap_feature_1),
                getString(R.string.iap_feature_3)
        };
        binding.viewPager.setAdapter(new IapFeatureAdapter(captions));
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
        updateDots(0);

        // Tự chuyển trang mỗi 3.5s (runnable tự re-post để chạy liên tục)
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding.viewPager.getAdapter() != null) {
                    int count = binding.viewPager.getAdapter().getItemCount();
                    if (count > 0) {
                        int next = (binding.viewPager.getCurrentItem() + 1) % count;
                        binding.viewPager.setCurrentItem(next, true);
                    }
                }
                autoScrollHandler.postDelayed(this, 3500);
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 3500);
    }

    private void updateDots(int pos) {
        View[] dots = {binding.dot1, binding.dot2, binding.dot3};
        for (int i = 0; i < dots.length; i++) {
            ViewGroup.LayoutParams lp = dots[i].getLayoutParams();
            lp.width = dpToPx(i == pos ? 30 : 6);
            dots[i].setLayoutParams(lp);
            dots[i].setBackgroundResource(i == pos
                    ? R.drawable.bg_iap_dot_active
                    : R.drawable.bg_iap_dot_inactive);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // Tính "Save X% · only $Y/week" cho gói Yearly từ giá micros (chỉ hiện khi đã có cả 2 giá)
    private void updateYearlySavings() {
        try {
            if (weeklyMicros > 0 && yearlyMicros > 0 && priceCurrency != null) {
                double yearlyPerWeek = (yearlyMicros / 1_000_000.0) / 52.0;
                double weekly = weeklyMicros / 1_000_000.0;
                int pct = (int) Math.round((1.0 - (yearlyPerWeek / weekly)) * 100.0);
                NumberFormat nf = NumberFormat.getCurrencyInstance();
                try {
                    Currency cur = Currency.getInstance(priceCurrency);
                    nf.setCurrency(cur);
                    int fd = cur.getDefaultFractionDigits();
                    nf.setMinimumFractionDigits(fd);
                    nf.setMaximumFractionDigits(fd);
                } catch (Exception ignored) {}
                String perWeekStr = nf.format(yearlyPerWeek);
                StringBuilder sb = new StringBuilder();
                if (pct > 0) sb.append(getString(R.string.iap_save, pct)).append("  ·  ");
                sb.append(getString(R.string.iap_price_per_week, perWeekStr));
                binding.tvYearlySave.setText(sb.toString());
                binding.tvYearlySave.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Khôi phục giao dịch (Restore Purchases) — bắt buộc theo policy Play
    private void restorePurchases() {
        try {
            if (billingClient == null) return;
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> runOnUiThread(() -> {
                        boolean found = false;
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                    found = true;
                                    verifySubPurchase(purchase);
                                }
                            }
                        }
                        if (!found) {
                            Toast.makeText(PremiumActivity.this, getString(R.string.iap_no_purchase), Toast.LENGTH_SHORT).show();
                        }
                    })
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MUA THẲNG: chọn offer KHÔNG có free-trial (mọi phase đều có giá > 0).
    // (Trước đây ưu tiên offer trial; nay bỏ trial theo yêu cầu → luôn tính tiền ngay.)
    private String pickOfferToken(ProductDetails productDetails) {
        try {
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
            if (offers == null || offers.isEmpty()) return "";
            for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                boolean hasFreePhase = false;
                for (ProductDetails.PricingPhase phase : offer.getPricingPhases().getPricingPhaseList()) {
                    if (phase.getPriceAmountMicros() == 0) {
                        hasFreePhase = true;
                        break;
                    }
                }
                if (!hasFreePhase) {
                    return offer.getOfferToken();
                }
            }
            // Nếu mọi offer đều kèm trial/intro, dùng offer cuối (thường là base plan gốc, trả tiền ngay).
            return offers.get(offers.size() - 1).getOfferToken();
        } catch (Exception e) {
            e.printStackTrace();
            return productDetails.getSubscriptionOfferDetails().get(0).getOfferToken();
        }
    }

    /**
     * First-run paywall dùng chung: hiện PremiumActivity (dismissible, extra "free_trial") ĐÚNG 1 LẦN
     * rồi vào Home; nếu đã hiện (cờ "first_paywall_shown") hoặc user đã Premium → vào thẳng Home.
     * Luôn finish() [from]. Dùng cho cả nhánh KHÔNG onboarding (SplashActivity) lẫn sau onboarding (ABOnBoarding).
     */
    public static void startFirstRunPaywallOrHome(android.app.Activity from) {
        boolean paywallShown = com.example.piceditor.utilsApp.PreferenceUtil.getInstance(from)
                .getValue("first_paywall_shown", false);
        boolean isPremium = new Prefs(from).getPremium() == 1;
        if (!paywallShown && !isPremium) {
            com.example.piceditor.utilsApp.PreferenceUtil.getInstance(from)
                    .setValue("first_paywall_shown", true);
            Intent i = new Intent(from, PremiumActivity.class);
            i.putExtra("free_trial", true);
            from.startActivity(i);
        } else {
            from.startActivity(new Intent(from, MainActivity.class));
        }
        from.finish();
    }

    @Override
    protected void onDestroy() {
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        super.onDestroy();
    }
}
