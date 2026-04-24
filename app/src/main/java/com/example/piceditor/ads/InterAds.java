package com.example.piceditor.ads;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class InterAds {


    private static final String INTER_TEST_ID = "ca-app-pub-3940256099942544/9999999999";

    private static final String INTER_ID_DEFAULT = "ca-app-pub-3607148519095421/2658258742";

    private static InterstitialAd mInterstitialAd;
    private static boolean isLoading = false;
    private static boolean isShowing = false;
    private static long loadTimeAd = 0;

    public static int flagQC = 1;

    public static void initInterAds(final Context ac, Callback callback) {
        if (isCanLoadAds()) {
            mInterstitialAd = null;
            isLoading = true;
            InterstitialAd.load(ac, BuildConfig.DEBUG ? INTER_TEST_ID : INTER_ID_DEFAULT, getAdRequest(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    mInterstitialAd = interstitialAd;
                    mInterstitialAd.setOnPaidEventListener(adValue -> {
                        try {
                            WeatherApplication.initROAS(adValue.getValueMicros(),adValue.getCurrencyCode());
                            AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                            adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                            Adjust.trackAdRevenue(adRevenue);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    isLoading = false;
                    loadTimeAd = (new Date()).getTime();
                    if (callback != null) {
                        callback.callback();
                    }
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    mInterstitialAd = null;
                    isLoading = false;
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

    public static void showAdsBreak(Activity activity, Callback callback) {

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
                showAdsFull((AppCompatActivity) activity, callback);
            } catch (Exception e) {
                e.printStackTrace();
                callback.callback();
            }
        } else {
            callback.callback();
        }
    }



    private static void showAdsFull(final AppCompatActivity context, final Callback callback) {
        FullScreenDialog fullScreenDialog = new FullScreenDialog();
        Toast toast = Toast.makeText(context,"CLICK HERE TO SKIP ADS >>>", Toast.LENGTH_LONG);
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                mInterstitialAd = null;
                isShowing = false;
                callback.callback();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                toast.cancel();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
                isShowing = true;
                toast.cancel();
                if (PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.CLICK_INTER, "no").equals("yes") && PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.HEHE, false)){
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                toast.show();
                            } catch (Exception e) {

                            }
                        }
                    },800);
                }
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                isShowing = false;
                mInterstitialAd = null;
                startDelay();
                initInterAds(context, null);
//                callback.callback();
                if (PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.NATIVE_AFTER_INTER, "no").equals("yes") && PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.HEHE, false)) {
                    fullScreenDialog.start();
                    if (fullScreenDialog.getDialog() != null) {
                        fullScreenDialog.requireDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (callback != null) {
                                    fullScreenDialog.dismiss();
                                    callback.callback();
                                }
                            }
                        });
                    } else {
                        if (callback != null) {
                            callback.callback();
                        }
                    }
                }else {
                    callback.callback();
                    Log.e("call111111111", "call4: "  );
                }
            }

        });
        mInterstitialAd.show(context);
        if (PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.NATIVE_AFTER_INTER, "no").equals("yes") && PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.HEHE, false)) {
            try {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fullScreenDialog.show(context.getSupportFragmentManager(), "FullScreenDialog");
                    }
                }, 300);
            } catch (Exception e) {

            }
        }
    }

    public static boolean isShowing() {
        return isShowing;
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable runnable = () -> flagQC = 1;

    public static void startDelay() {

        String s = PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.INTER_TIME, "20000");
        int inter_time = 20000;
        try {
            inter_time = Integer.parseInt(s);
        } catch (Exception e) {
            inter_time = 20000;
        }



        flagQC = 0;
        handler.removeCallbacks(runnable);
        if (PreferenceUtil.getInstance(WeatherApplication.get()).getValue(Constant.SharePrefKey.HEHE, false)) {
            handler.postDelayed(runnable, BuildConfig.DEBUG ? 1000 : inter_time);
        }else {
            handler.postDelayed(runnable, BuildConfig.DEBUG ? 1000 : 45000);
        }

//        Toast.makeText(WeatherApplication.get(), inter_time+"", Toast.LENGTH_SHORT).show();

}}
