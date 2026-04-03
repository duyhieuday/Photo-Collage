package com.example.piceditor.obd;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.example.piceditor.BuildConfig;
import com.example.piceditor.R;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.ads.IdAds;
import com.example.piceditor.databinding.AdUnifiedLanguageBinding;
import com.example.piceditor.utils.BarsUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;

public class ABOnBoardingFragment3 extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private NavigationHost navigationHost;
    private FrameLayout adFrame;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationHost) {
            navigationHost = (NavigationHost) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement NavigationHost");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ab_fragment_on_boarding_3, container, false);
        adFrame = view.findViewById(R.id.ad_frame);

        BarsUtils.setStatusBarColor(requireActivity(), Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(requireActivity(), true);

        refreshAd();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUI();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(requireActivity().getWindow(), requireActivity().getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
    }

    private static final String ADMOB_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110";
    private NativeAd currentNativeAd;
    private void populateNativeAdView(NativeAd nativeAd, AdUnifiedLanguageBinding adView) {
        NativeAdView nativeAdView = adView.rootNative;

        // Set the media view.
        nativeAdView.setMediaView(nativeAdView.findViewById(R.id.ad_media));

        // Set other ad assets.
        nativeAdView.setHeadlineView(adView.adHeadline);
        nativeAdView.setBodyView(adView.adBody);
        nativeAdView.setCallToActionView(adView.adCallToAction);
        nativeAdView.setIconView(adView.adAppIcon);
        nativeAdView.setStarRatingView(adView.adStars);
        nativeAdView.setAdvertiserView(adView.adAdvertiser);
        nativeAdView.setMediaView(adView.adMedia);

        adView.adHeadline.setText(nativeAd.getHeadline());

        // Set media content if available
        if (nativeAd.getMediaContent() != null) {
            adView.adMedia.setMediaContent(nativeAd.getMediaContent());
        }

        if (nativeAd.getBody() == null) {
            adView.adBody.setVisibility(View.GONE);
        } else {
            adView.adBody.setVisibility(View.VISIBLE);
            adView.adBody.setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.adCallToAction.setVisibility(View.INVISIBLE);
        } else {
            adView.adCallToAction.setVisibility(View.VISIBLE);
            adView.adCallToAction.setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.adAppIcon.setVisibility(View.GONE);
        } else {
            adView.adAppIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.adAppIcon.setVisibility(View.VISIBLE);
        }

// Set star rating visibility and value
        if (nativeAd.getStarRating() == null) {
            adView.adStars.setVisibility(View.GONE);
        } else {
            adView.adStars.setRating(nativeAd.getStarRating().floatValue());
            adView.adStars.setVisibility(View.GONE);
        }

// Set advertiser visibility and text
        if (nativeAd.getAdvertiser() == null) {
            adView.adAdvertiser.setVisibility(View.GONE);
        } else {
            adView.adAdvertiser.setText(nativeAd.getAdvertiser());
            adView.adAdvertiser.setVisibility(View.VISIBLE);
        }

        nativeAdView.setNativeAd(nativeAd);

        VideoController vc = nativeAd.getMediaContent().getVideoController();
        if (vc != null && vc.hasVideoContent()) {
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        }
    }
    private void refreshAd() {

        AdLoader.Builder builder = new AdLoader.Builder(requireActivity(), BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_OBD3_ID);

        builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                try {
                    if (!requireActivity().isDestroyed() && !requireActivity().isFinishing() && !requireActivity().isChangingConfigurations()) {
                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(@NonNull AdValue adValue) {
                                try {
                                    WeatherApplication.initROAS(adValue.getValueMicros(), adValue.getCurrencyCode());
                                    AdjustAdRevenue adRevenue = new AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB);
                                    adRevenue.setRevenue((double) (adValue.getValueMicros() / 1000000f), adValue.getCurrencyCode());
                                    Adjust.trackAdRevenue(adRevenue);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        currentNativeAd = nativeAd;
                        AdUnifiedLanguageBinding unifiedAdBinding = AdUnifiedLanguageBinding.inflate(getLayoutInflater());
                        populateNativeAdView(nativeAd, unifiedAdBinding);
                        adFrame.removeAllViews();
                        adFrame.addView(unifiedAdBinding.getRoot());
                    } else {
                        nativeAd.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                AdError adError = loadAdError.getCause();
                String error = "Domain: " + loadAdError.getDomain() + ", Code: " + loadAdError.getCode() + ", Message: " + loadAdError.getMessage();
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());

    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationHost = null;
    }


}