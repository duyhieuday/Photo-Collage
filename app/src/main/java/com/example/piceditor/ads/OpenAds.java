package com.example.piceditor.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.example.piceditor.BuildConfig;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.splash.SplashActivity;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

public class OpenAds {

    private static final String OPEN_TEST_ID = "ca-app-pub-3940256099942544/9257395921";

    private static final String OPEN_ID_DEFAULT = "ca-app-pub-3607148519095421/2245171681";

    private static AppOpenAd appOpenAd;

    private static boolean isOpenShowingAd = false;

    private static long loadTimeOpenAd = 0;
    public static int flagQC = 1;

    public static void initOpenAds(final Context ac, Callback callback) {

        if (appOpenAd == null || !isOpenAdsCanUse()) {
            appOpenAd = null;
            AppOpenAd.load(ac, BuildConfig.DEBUG ? OPEN_TEST_ID : OPEN_ID_DEFAULT, getAdRequest(),  new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd openAd) {
                    super.onAdLoaded(openAd);
                    appOpenAd = openAd;
                    appOpenAd.setOnPaidEventListener(adValue -> {
                        try {
                            WeatherApplication.initROAS(adValue.getValueMicros(),adValue.getCurrencyCode());
                            AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                            adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                            Adjust.trackAdRevenue(adRevenue);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    loadTimeOpenAd = (new Date()).getTime();
                    if (callback != null) {
                        callback.callback();
                    }
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    appOpenAd = null;
                    if (callback != null) {
                        callback.callback();
                    }
                }
            });
        } else {
            if (callback != null) {
                callback.callback();
            }
        }
    }

    private static boolean retryShowOpenAds = false;

    private static boolean isOpenAdsCanUse() {
        long dateDifference = (new Date()).getTime() - loadTimeOpenAd;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * 4));
    }

    private static AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    public static boolean isCanShowOpenAds() {
        return appOpenAd != null && !isOpenShowingAd && isOpenAdsCanUse();
    }

    public static void showOpenAds(final Activity context, final Callback callback) {
        if(!PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.HEHE, false)){
            callback.callback();
            return;
        }
        try {
            int isPro = new Prefs(WeatherApplication.get()).getPremium();
            boolean isSub = new Prefs(WeatherApplication.get()).isRemoveAd();
            if (isPro == 1) {
                callback.callback();
                return;
            } else if (isSub) {
                callback.callback();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (flagQC == 1) {
            if (isCanShowOpenAds()) {
                if(PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
                    if (!(context instanceof SplashActivity)) {
//                        OverLay.startScreen(context);
                    }
                }
                appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        if (retryShowOpenAds) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> showOpenAds(context, callback), 500);
                            retryShowOpenAds = false;
                        } else {
                            appOpenAd = null;
                            callback.callback();
                        }
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        isOpenShowingAd = true;
                        retryShowOpenAds = false;
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        isOpenShowingAd = false;
                        appOpenAd = null;
                        retryShowOpenAds = false;
                        initOpenAds(context, null);
                        InterAds.startDelay();
                        startDelay();
                        callback.callback();
                        if(PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")) {
//                            OverLay.finishActivity();
                        }
                    }

                });
                appOpenAd.show(context);
                retryShowOpenAds = true;
            } else {
                callback.callback();
            }
        } else {
            callback.callback();
        }
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable runnable = () -> flagQC = 1;

    public static void startDelay() {
        flagQC = 0;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, BuildConfig.DEBUG ? 15000 : 1000 * 60);
    }

    public static class CloseOpenAds{

    }

    public static class OpenOpenAds{

    }

}
