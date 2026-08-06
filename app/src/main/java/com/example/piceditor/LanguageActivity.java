package com.example.piceditor;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.example.piceditor.adapters.LanguageAdapter;
import com.example.piceditor.ads.Callback;
import com.example.piceditor.ads.IdAds;
import com.example.piceditor.ads.InterAds;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivityLanguageBinding;
import com.example.piceditor.databinding.AdUnifiedLanguage1Binding;
import com.example.piceditor.databinding.AdUnifiedLanguageBinding;
import com.example.piceditor.model.LanguageApp;
import com.example.piceditor.model.LanguageListener;
import com.example.piceditor.obd.ABOnBoardingActivity;
import com.example.piceditor.splash.SplashActivity;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.LanguageManager;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.example.piceditor.utilsApp.Prefs;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

public class LanguageActivity extends BaseActivityNew<ActivityLanguageBinding> {

    private LanguageAdapter languageAdapter;
    private List<LanguageApp> languageList;
    private SharedPreferences sharedPreferences;
    private NativeAd currentNativeAd;
    private NativeAd currentNativeAd2;
    private boolean isNative = true;

    /** Đánh dấu vào màn này từ SplashActivity (luồng first flow), phân biệt với vào từ SettingActivity. */
    public static final String EXTRA_FROM_SPLASH = "from_splash";
    private boolean fromSplash;
    /** Chặn bấm tick lần 2 khi nhánh INTER_Language đang chờ inter đóng (tránh mở 2 lần OnBoarding). */
    private boolean isNavigating;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_language;
    }

    @Override
    public int getFrame() {
        return 0;
    }

    @Override
    public void getDataFromIntent() {

    }

    @Override
    public void doAfterOnCreate() {

    }

    @Override
    public void setListener() {

    }

    @Override
    public BaseFragment initFragment() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vào từ Splash (lần đầu chạy hoặc remote first_flow) → chọn xong đi tiếp OnBoarding.
        // Vào từ Setting → chỉ đổi ngôn ngữ rồi về Home, không kéo lại onboarding.
        fromSplash = getIntent().getBooleanExtra(EXTRA_FROM_SPLASH, false);

        BarsUtils.setStatusBarColor(this, Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(this, true);
        BarsUtils.setHideNavigation(this);

        getBinding().icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Gson gson = new Gson();
        Type language = new TypeToken<List<LanguageApp>>() {
        }.getType();
        List<LanguageApp> languages = null;
        try {
            languages = gson.fromJson(new InputStreamReader(this.getAssets().open("data/languages.json")), language);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getBinding().rcvLanguage.setLayoutManager(new LinearLayoutManager(
                this, RecyclerView.VERTICAL, false
        ));

        languageList = languages;
        languageAdapter = new LanguageAdapter();
        languageAdapter.setData(languageList);

        getBinding().rcvLanguage.setAdapter(languageAdapter);
        getBinding().rcvLanguage.smoothScrollToPosition(0);

        Log.e("cxcc", "languages: " + languages);
        Log.e("cxcc", "languageList: " + languageList);

        languageAdapter.setLanguageListener(new LanguageListener() {
            @Override
            public void onLanguageClick(int position, LanguageApp language) {
                Prefs.putString(Prefs.Key.LANGUAGE_NAME, language.getLanguageName());
                sharedPreferences = getSharedPreferences("signLanguage", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("getSignLanguage", language.getSignLanguage());
                editor.apply();
                if (PreferenceUtil.getInstance(LanguageActivity.this)
                        .getValue(Constant.SharePrefKey.delay_tick, "no").equals("yes")) {
                    // Giống translate: giữ tick ẩn 2s rồi fade-in, để user ở lại với native ad lâu hơn.
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (getBinding().iconTick.getVisibility() != View.VISIBLE) {
                                    getBinding().iconTick.setVisibility(View.VISIBLE);
                                    getBinding().iconTick.setAlpha(0f);
                                    ObjectAnimator.ofFloat(getBinding().iconTick, View.ALPHA, 0f, 1f)
                                            .setDuration(300).start();
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }, 2000);
                } else {
                    getBinding().iconTick.setVisibility(View.VISIBLE);
                }
                if (isNative) {
                    getBinding().adFrame1.setVisibility(View.VISIBLE);
                    isNative = false;
                }
            }
        });

        LanguageManager languageManager = new LanguageManager(LanguageActivity.this);
        getBinding().iconTick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNavigating) return;
                isNavigating = true;
                sharedPreferences = getSharedPreferences("signLanguage", MODE_PRIVATE);
                String signLanguage = sharedPreferences.getString("getSignLanguage", null);
                if (signLanguage == null) {
                    String systemLanguage = Locale.getDefault().getLanguage();
                    languageManager.updateResource(systemLanguage);
                } else {
                    languageManager.updateResource(signLanguage);
                }
                // KHÔNG đánh dấu first flow đã xong ở đây. Mốc đó nằm ở MainActivity
                // (Prefs.Key.KEY_FIRST_FLOW_DONE): user thoát app giữa OnBoarding/paywall thì
                // lần mở sau vẫn phải được chạy lại Language + OnBoarding từ đầu.

                if (!fromSplash) {
                    // Vào từ Setting: chỉ đổi ngôn ngữ rồi về Home.
                    startActivity(new Intent(LanguageActivity.this, MainActivity.class));
                    finish();
                    return;
                }

                Prefs.putBoolean(Prefs.Key.FIRST_ONBOARDING, false);
                if (PreferenceUtil.getInstance(LanguageActivity.this)
                        .getValue(Constant.SharePrefKey.INTER_Language, "no").equals("yes")) {
                    InterAds.showAdsBreak(LanguageActivity.this, new Callback() {
                        @Override
                        public void callback() {
                            goOnBoarding();
                        }
                    });
                } else {
                    goOnBoarding();
                }
            }
        });

        refreshAd();
        refreshAd1();

    }

    private void goOnBoarding() {
        startActivity(new Intent(LanguageActivity.this, ABOnBoardingActivity.class));
        finish();
    }

    private void populateNativeAdView(NativeAd nativeAd, AdUnifiedLanguageBinding adView) {
        NativeAdView nativeAdView = adView.rootNative;

        // Set the media view.
        nativeAdView.setMediaView(findViewById(R.id.ad_media));

        // Set other ad assets.
        nativeAdView.setHeadlineView(adView.adHeadline);
        nativeAdView.setBodyView(adView.adBody);
        nativeAdView.setCallToActionView(adView.adCallToAction);
        nativeAdView.setIconView(adView.adAppIcon);
        nativeAdView.setStarRatingView(adView.adStars);
        nativeAdView.setAdvertiserView(adView.adAdvertiser);

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
        if (SplashActivity.currentNativeAd != null && !isDestroyed() && !isFinishing() && !isChangingConfigurations()) {
            AdUnifiedLanguageBinding unifiedAdBinding = AdUnifiedLanguageBinding.inflate(getLayoutInflater());
            populateNativeAdView(SplashActivity.currentNativeAd, unifiedAdBinding);
            getBinding().adFrame.removeAllViews();
            getBinding().adFrame.addView(unifiedAdBinding.getRoot());
            return;
        }

        AdLoader.Builder builder = new AdLoader.Builder(this, BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_LANGUAGES1);

        builder.forNativeAd(new com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                try {
                    if (!isDestroyed() && !isFinishing() && !isChangingConfigurations()) {
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
                        getBinding().adFrame.removeAllViews();
                        getBinding().adFrame.addView(unifiedAdBinding.getRoot());
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

    private static final String ADMOB_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110";

    private void populateNativeAdView1(NativeAd nativeAd, AdUnifiedLanguage1Binding adView) {
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

    private void refreshAd1() {

        AdLoader.Builder builder = new AdLoader.Builder(LanguageActivity.this, BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_LANGUAGES2);

        builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
            @Override
            public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                try {
                    if (!isDestroyed() && !isFinishing() && !isChangingConfigurations()) {
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

                        currentNativeAd2 = nativeAd;
                        AdUnifiedLanguage1Binding unifiedAdBinding = AdUnifiedLanguage1Binding.inflate(getLayoutInflater());
                        populateNativeAdView1(nativeAd, unifiedAdBinding);
                        getBinding().adFrame1.removeAllViews();
                        getBinding().adFrame1.addView(unifiedAdBinding.getRoot());

                        Log.e("xnncs1sa", "Show native2 successfully");
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
                Log.e("xnncs1sa", "Show native2 fail");
            }
        }).build();

        adLoader.loadAd(new AdRequest.Builder().build());

    }

}