package com.example.piceditor.ads;

import android.content.Context;
import android.util.Log;

import com.example.piceditor.BuildConfig;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import org.greenrobot.eventbus.EventBus;

public class RefreshAds {
    private static final String ADMOB_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110";
    public static NativeAd currentNativeOBD1;
    public static void refreshAdOBD1(Context context) {
        AdLoader.Builder builder = new AdLoader.Builder(context,
                BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_OBD1_ID);
        builder.forNativeAd(nativeAd -> {
            if (currentNativeOBD1 != null) {
                currentNativeOBD1.destroy();
            }
            currentNativeOBD1 = nativeAd;
            EventBus.getDefault().post(new AdEvent(true, "Ad loaded successfully"));
        });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                String error = "domain: " + loadAdError.getDomain() + ", code: " + loadAdError.getCode() + ", message: " + loadAdError.getMessage();
                Log.e("onAdLoaded: Fail", "onAdFailedToLoad: " + error );
                EventBus.getDefault().post(new AdEvent(false, error));
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static NativeAd currentNativeOBD3;
    public static void refreshAdOBD3(Context context) {
        AdLoader.Builder builder = new AdLoader.Builder(context,
                BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_OBD3_ID);
        builder.forNativeAd(nativeAd -> {
            if (currentNativeOBD3 != null) {
                currentNativeOBD3.destroy();
            }
            currentNativeOBD3 = nativeAd;
        });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                String error = "domain: " + loadAdError.getDomain() + ", code: " + loadAdError.getCode() + ", message: " + loadAdError.getMessage();
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

}
