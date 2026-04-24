package com.example.piceditor.ads;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.example.piceditor.BuildConfig;
import com.example.piceditor.R;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;


public class BannerAds {

    private static final String BANNER_TEST_ID = "ca-app-pub-3940256099942544/9999999999";

    private static final String BANNER_ID_DEFAULT = "ca-app-pub-3607148519095421/3095218929";

    public static AdSize getAdSize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }


    public static void clearBanner(Activity ctx) {
        try {
            final LinearLayout adViewContainer = ctx.findViewById(R.id.adView_container);
            if (adViewContainer != null) {
                adViewContainer.removeAllViews();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearBanner(boolean isShowBanner) {
        if(PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")){

        }else {
            int isPro = new Prefs(WeatherApplication.get()).getPremium();
            boolean isSub = new Prefs(WeatherApplication.get()).isRemoveAd();
            if (isPro == 1) {
                if (viewRoot != null) {
                    viewRoot.setVisibility(View.GONE);
                }
                return;
            } else if (isSub) {
                if (viewRoot != null) {
                    viewRoot.setVisibility(View.GONE);
                }
                return;
            }
            try {
                if (viewRoot != null) {
                    if (isShowBanner) {
                        viewRoot.setVisibility(View.VISIBLE);
                    } else
                        viewRoot.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    public static void initBannerAds(Activity ctx) {
        try {
            final AdView mAdViewBanner = new AdView(ctx);
            mAdViewBanner.setAdSize(getAdSize(ctx));
            mAdViewBanner.setAdUnitId(BuildConfig.DEBUG ? BANNER_TEST_ID : BANNER_ID_DEFAULT);
            final LinearLayout adViewContainer = ctx.findViewById(R.id.adView_container);
            if (adViewContainer == null)
                return;

            adViewContainer.removeAllViews();
            adViewContainer.addView(mAdViewBanner);
            final AdRequest adRequest = getAdRequest();
            mAdViewBanner.loadAd(adRequest);
            mAdViewBanner.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    mAdViewBanner.setOnPaidEventListener(adValue -> {
                        try {
                            WeatherApplication.initROAS(adValue.getValueMicros(), adValue.getCurrencyCode());
                            AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                            adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                            Adjust.trackAdRevenue(adRevenue);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    adViewContainer.setVisibility(View.VISIBLE);
                    hideBannerLoading(ctx, false);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    adViewContainer.setVisibility(View.GONE);
                    hideBannerLoading(ctx, true);
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    adViewContainer.setVisibility(View.GONE);
                    hideBannerLoading(ctx, true);
                }

            });

            hideBannerLoading(ctx, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isInitBanner;
    private static View viewRoot;

    public static View getViewRoot() {
        return viewRoot;
    }

    public static void initBannerAdsOptimize(Activity ctx) {
        if(!PreferenceUtil.getInstance(ctx).getValue(Constant.SharePrefKey.HEHE, false)){
            return;
        }
        if (!isInitBanner) {
            try {
                viewRoot = LayoutInflater.from(ctx).inflate(R.layout.ab_banner_ads, null);
                final AdView mAdViewBanner = new AdView(ctx);
                mAdViewBanner.setAdSize(getAdSize(ctx));
                mAdViewBanner.setAdUnitId(BuildConfig.DEBUG ? BANNER_TEST_ID : BANNER_ID_DEFAULT);
                final LinearLayout adViewContainer = viewRoot.findViewById(R.id.adView_container);
                if (adViewContainer == null)
                    return;

                adViewContainer.removeAllViews();
                adViewContainer.addView(mAdViewBanner);
                final AdRequest adRequest = getAdRequest();
                mAdViewBanner.loadAd(adRequest);
                mAdViewBanner.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        isInitBanner = true;
                        mAdViewBanner.setOnPaidEventListener(adValue -> {
                            try {
                                WeatherApplication.initROAS(adValue.getValueMicros(), adValue.getCurrencyCode());
                                AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                                adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                                Adjust.trackAdRevenue(adRevenue);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        adViewContainer.setVisibility(View.VISIBLE);
                        hideBannerLoading(viewRoot, false, ctx);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        isInitBanner = false;
                        adViewContainer.setVisibility(View.GONE);
                        hideBannerLoading(viewRoot, true, ctx);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        isInitBanner = false;
                        adViewContainer.setVisibility(View.GONE);
                        hideBannerLoading(viewRoot, true, ctx);
                    }

                });

                hideBannerLoading(viewRoot, false, ctx);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static void hideBannerLoading(View view, boolean bl, Activity ctx) {
        try {
            TextView tvBannerLoading = view.findViewById(R.id.tv_loading);
            tvBannerLoading.setMinimumHeight(getAdSize(ctx).getHeightInPixels(ctx));
            if (bl) {
                tvBannerLoading.setVisibility(View.GONE);
//                view.findViewById(R.id.view_d).setVisibility(View.GONE);
            } else {
                tvBannerLoading.setVisibility(View.VISIBLE);
//                view.findViewById(R.id.view_d).setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideBannerLoading(Activity ctx, boolean bl) {
        try {
            TextView tvBannerLoading = ctx.findViewById(R.id.tv_loading);
            tvBannerLoading.setMinimumHeight(getAdSize(ctx).getHeightInPixels(ctx));
            if (bl) {
                tvBannerLoading.setVisibility(View.GONE);
//                ctx.findViewById(R.id.view_d).setVisibility(View.GONE);
            } else {
                tvBannerLoading.setVisibility(View.VISIBLE);
//                ctx.findViewById(R.id.view_d).setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
