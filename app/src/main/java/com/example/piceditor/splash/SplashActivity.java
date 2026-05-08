package com.example.piceditor.splash;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.example.piceditor.LanguageActivity;
import com.example.piceditor.MainActivity;
import com.example.piceditor.R;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.ads.GDPRRequestable;
import com.example.piceditor.ads.InterAds;
import com.example.piceditor.ads.InterAdsSplash;
import com.example.piceditor.ads.NativeFullScreen;
import com.example.piceditor.ads.OpenAds;
import com.example.piceditor.base.BaseActivityNew;
import com.example.piceditor.base.BaseFragment;
import com.example.piceditor.databinding.ActivitySplashBinding;
import com.example.piceditor.utils.BarsUtils;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.example.piceditor.utilsApp.Prefs;
import com.google.android.gms.ads.nativead.NativeAd;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivityNew<ActivitySplashBinding> {

    private boolean isIntented = false;
    public static NativeAd currentNativeAd;

    @Override
    public int getLayoutRes() {
        return R.layout.activity_splash;
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
        initGDPR();
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
        BarsUtils.setStatusBarColor(this, Color.parseColor("#01000000"));
        BarsUtils.setAppearanceLightStatusBars(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BarsUtils.setHideNavigation(SplashActivity.this);
    }

    private void intent(String debug) {
        boolean isFirstRun = Prefs.getBoolean(Prefs.Key.KEY_LANGUAGE, true);
        if (!isIntented) {
            if (PreferenceUtil.getInstance(getApplicationContext()).getValue(Constant.SharePrefKey.HEHE, false)) {
                if (isFirstRun) {
                    startActivity(new Intent(this, LanguageActivity.class));
                    finish();
                } else {
                    if(PreferenceUtil.getInstance(getApplicationContext()).getValue(Constant.SharePrefKey.FIRST_FLOW, "no").equals("yes")){
                        startActivity(new Intent(this, LanguageActivity.class));
                        finish();
                    }else {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                }
            } else {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            isIntented = true;
        }
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager != null ? manager.getActiveNetworkInfo() : null;
        return networkInfo != null && networkInfo.isConnected();
    }

    private void initAds() {
        if (isNetworkAvailable(this)) {
            InterAdsSplash.initInterAds(this, () -> {
                InterAdsSplash.showAdsBreakWithoutNT(this, () -> intent("1"));
                WeatherApplication.trackingEvent("inter_splash");
            });
            OpenAds.initOpenAds(this, () -> {
            });
            InterAds.initInterAds(this, null);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> intent("1"), 400);
        }
    }

    private void initGDPR() {
        if (isNetworkAvailable(this)) {
            WeatherApplication.trackingEvent("check_GDPR");
            GDPRRequestable.getGdprRequestable(this).setOnRequestGDPRCompleted(formError -> {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    DC.init(
//                            getApplicationContext(),
//                            BuildConfig.DEBUG ? ADMOB_AD_UNIT_ID_TEST : IdAds.NATIVE_IN_APP
//                    );
//                } else {
//                    PreferenceUtil.getInstance(WeatherApplication.get())
//                            .setValue(Constant.SharePrefKey.HEHE, false);
//                }
                currentNativeAd = null;
                initAds();
                NativeFullScreen.loadNative(this);
            });
            GDPRRequestable.getGdprRequestable(this).requestGDPR();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> intent("ex"), 100);
        }
    }

    private static final String ADMOB_AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/2247696110";
}