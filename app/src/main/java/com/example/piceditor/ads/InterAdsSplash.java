package com.example.piceditor.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.example.piceditor.BuildConfig;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.Date;

public class InterAdsSplash {

    public static boolean firstAction = true;
    private static final String INTER_TEST_ID = "ca-app-pub-3940256099942544/9999999999";
    private static String currentAdId;
    private static InterstitialAd mInterstitialAd;
    private static boolean isLoading = false;
    private static boolean isShowing = false;
    private static long loadTimeAd = 0;
    public static int flagQC = 1;

    public static void initInterAds(final Context ac, Callback callback) {
        if (isCanLoadAds()) {
            mInterstitialAd = null;
            isLoading = true;
            currentAdId = IdAds.INTER_SPLASH_HIGH_ID;
            loadInterAd(ac, currentAdId, callback);
        } else {
            if (callback != null) {
                callback.callback();
            }
        }
    }
    private static void loadInterAd(final Context ac, String adId, Callback callback) {
        InterstitialAd.load(ac, BuildConfig.DEBUG ? INTER_TEST_ID : adId, getAdRequest(), new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                mInterstitialAd = interstitialAd;
                mInterstitialAd.setOnPaidEventListener(adValue -> {
                    try {
                        WeatherApplication.initROAS(adValue.getValueMicros(), adValue.getCurrencyCode());
                        AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                        adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                        Adjust.trackAdRevenue(adRevenue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                isLoading = false;
                loadTimeAd = (new Date()).getTime();
                Log.e("InterAdsSplash", "Load success inter splash: " + currentAdId);
                if (callback != null) {
                    callback.callback();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                if (currentAdId.equals(IdAds.INTER_SPLASH_HIGH_ID)) {
                    currentAdId = IdAds.INTER_SPLASH_ID;
                    Log.e("InterAdsSplash", "ID 1 failed, trying ID 2: " + currentAdId);
                    loadInterAd(ac, currentAdId, callback);
                } else {
                    Log.e("InterAdsSplash", "Both ad IDs failed to load");
                    mInterstitialAd = null;
                    isLoading = false;
                    if (callback != null) {
                        callback.callback();
                    }
                }
            }
        });
    }

    private static AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    private static boolean isCanLoadAds() {
        if (isLoading) {
            return false;
        }
        if (isShowing) {
            return false;
        }
        if (mInterstitialAd == null) {
            return true;
        } else {
            return isAdsOverdue();
        }
    }

    public static boolean isCanShowAds() {
        if (isLoading) {
            return false;
        }
        if (isShowing) {
            return false;
        }
        if (flagQC == 0) {
            return false;
        }
        if (mInterstitialAd == null) {
            return false;
        } else {
            return !isAdsOverdue();
        }

    }

    private static boolean isAdsOverdue() {
        long dateDifference = (new Date()).getTime() - loadTimeAd;
        long numMilliSecondsPerHour = 3600000;
        return dateDifference > (numMilliSecondsPerHour * (long) 4);
    }

    public static void showAdsBreakWithoutNT(Activity activity, Callback callback) {
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
        if (isCanShowAds()) {
//            Dialog dialog = new Dialog(activity, R.style.Dialog90);
//            dialog.setContentView(R.layout.inter_dialog_loading);
//            dialog.setCancelable(false);
//            dialog.show();
//            Handler handler = new Handler(Looper.getMainLooper());
//            Runnable runnable = () -> {
//                if(!activity.isFinishing()){
//                    dialog.dismiss();
//                    showAdsFull(activity, callback);
//                }
//            };
//            handler.postDelayed(runnable, 1300);
            try {
                showAdsFullWithoutNT(activity, callback);
            } catch (Exception e) {
                e.printStackTrace();
                callback.callback();
            }
        } else {
            callback.callback();
        }
    }

    private static void showAdsFullWithoutNT(final Activity context, final Callback callback) {
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                mInterstitialAd = null;
                isShowing = false;
                callback.callback();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                isShowing = true;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                isShowing = false;
                mInterstitialAd = null;
                startDelay();
//                initInterAds(context, null);
                callback.callback();
                if (PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.HEHE, false)) {

                }else{
                    InterAds.startDelay();
                }


            }

        });
        mInterstitialAd.show(context);

    }

    public static boolean isShowing() {
        return isShowing;
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable runnable = () -> flagQC = 1;

    public static void startDelay() {
        flagQC = 0;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, BuildConfig.DEBUG ? 10000 : 20000);
    }

}
