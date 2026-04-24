package com.example.piceditor.ads;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.example.piceditor.BuildConfig;
import com.example.piceditor.R;
import com.example.piceditor.utilsApp.ExtentionsKt;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

import java.util.ArrayList;

public class NativeFullScreen {
    private static final String NATIVE_TEST_ID = "ca-app-pub-3940256099942544/9999999999";
    private static final String NATIVE_ID = IdAds.NATIVE_IN_APP;

    private static final ArrayList<NativeAd> nativeAds = new ArrayList<>();

    public static void loadNative(Context context) {

//        Prefs prefs = new Prefs(context);
//        if (prefs.getPremium()) {
//            return;
//        }
        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();

        NativeAdOptions adOptions =
                new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        AdLoader adLoader = new AdLoader.Builder(context, BuildConfig.DEBUG ? NATIVE_TEST_ID : NATIVE_ID)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        nativeAds.add(nativeAd);

                    }
                }).withNativeAdOptions(adOptions)
                .build();
        new Thread(() -> adLoader.loadAd(getAdRequest())).start();
    }

    private static AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    public static void showNative(Context context, View ctx, String vitri) {
//        Prefs prefs = new Prefs(context);
//        if (prefs.getPremium()) {
//            ctx.findViewById(R.id.native_ads).setVisibility(View.GONE);
//            return;
//        }
        if (nativeAds.size() == 0) {
            ctx.findViewById(R.id.native_ads).setVisibility(View.GONE);
        } else {
            NativeAd ads = nativeAds.get(nativeAds.size() - 1);
            ads.setOnPaidEventListener(adValue -> {
                try {
                    ExtentionsKt.logFlurry(context, "Native_" + vitri, adValue.getValueMicros(), adValue.getCurrencyCode());
                    AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                    adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                    Adjust.trackAdRevenue(adRevenue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            loadNative(context);

            View view = ctx.findViewById(R.id.native_ads);
            NativeAdView adView = view.findViewById(R.id.ad_view);
            adView.setVisibility(View.VISIBLE);
            MediaView mediaView = adView.findViewById(R.id.ad_media);
            TextView tvHeadLine = adView.findViewById(R.id.ad_headline);
            TextView tvBody = adView.findViewById(R.id.ad_body);
            TextView tvAction = adView.findViewById(R.id.ad_call_to_action);
            ImageView imgIcon = adView.findViewById(R.id.ad_icon);
//            AppCompatImageView imgDrop = adView.findViewById(R.id.iv_drop);
            adView.setMediaView(mediaView);
            adView.setHeadlineView(tvHeadLine);
            adView.setBodyView(tvBody);
            adView.setCallToActionView(tvAction);
            adView.setIconView(imgIcon);
            tvHeadLine.setText(ads.getHeadline() != null ? ads.getHeadline() : "");
            tvBody.setText(ads.getBody() != null ? ads.getBody() : "");
            tvAction.setText(ads.getCallToAction() != null ? ads.getCallToAction() : "");

            CardView cvIcon = adView.findViewById(R.id.cv_icon);
            if (ads.getIcon() != null) {
                if (ads.getIcon().getDrawable() != null) {
                    cvIcon.setVisibility(View.VISIBLE);
                    imgIcon.setImageDrawable(ads.getIcon().getDrawable());
                } else if (ads.getIcon().getUri() != null) {
                    cvIcon.setVisibility(View.VISIBLE);
                    imgIcon.setImageURI(ads.getIcon().getUri());
                } else {
                    cvIcon.setVisibility(View.GONE);
                }
            } else {
                cvIcon.setVisibility(View.GONE);
            }
//            if (imgDrop != null){
//                imgDrop.setOnClickListener(view1 -> {
//                    mediaView.setVisibility(View.GONE);
//                    imgDrop.setVisibility(View.GONE);
//                });
//            }
            adView.setNativeAd(ads);
//            if (ads.getHeadline().contains("Test Ad")){
//                adView.setVisibility(View.GONE);
//            }
        }


    }

}
