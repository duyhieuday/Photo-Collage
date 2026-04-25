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
public class SplashActivityTest extends BaseActivityNew<ActivitySplashBinding> {

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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                intent("");
            }
        }, 1000);
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
        BarsUtils.setHideNavigation(SplashActivityTest.this);
    }

    private void intent(String debug) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }


}