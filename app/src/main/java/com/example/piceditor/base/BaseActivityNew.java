package com.example.piceditor.base;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.example.piceditor.WeatherApplication;
import com.example.piceditor.ads.BannerAds;
import com.example.piceditor.ads.BannerCollapsibleAds;
import com.example.piceditor.ads.Prefs;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.PreferenceUtil;

import java.util.Locale;


public abstract class BaseActivityNew<T extends ViewDataBinding> extends BaseActivityBlank {

    public abstract int getLayoutRes();
    public abstract int getFrame();
    public abstract void getDataFromIntent();
    public abstract void doAfterOnCreate();
    public abstract void setListener();
    public abstract BaseFragment initFragment();
    private T binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("create1", "onCreate: " );

        try {
            try {
                SharedPreferences sharedPreferences = getSharedPreferences("signLanguage", MODE_PRIVATE);
                String signLanguage = sharedPreferences.getString("getSignLanguage", null);
                if (signLanguage != null) {
                    Locale locale = new Locale(signLanguage);
                    Locale.setDefault(locale);
                    Resources resources = getResources();
                    Configuration configuration = resources.getConfiguration();
                    configuration.setLocale(locale);
                    resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                }
            } catch (Exception e) {

            }

            if(PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")){

            }else {
                BannerAds.initBannerAdsOptimize(this);
            }
            doFirstMethod();
            afterSetContentView();
            binding = DataBindingUtil.setContentView(this, getLayoutRes());
            getDataFromIntent();
            doAfterOnCreate();
            setListener();
            getSupportFragmentManager().beginTransaction().replace(getFrame(), initFragment()).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doFirstMethod() {

    }

    public T getBinding(){
        return binding;
    }

    public void afterSetContentView(){

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Ticket", "onPause");
//        WeatherApplication.get().onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Ticket", "onResume");
//        WeatherApplication.get().onResume();
    }

    public void initBanner(ViewGroup viewGroup){
        if(!PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.HEHE, false)){
            return;
        }
        Log.e("TAG", "initBanner: " );
        if(PreferenceUtil.getInstance(this).getValue(Constant.SharePrefKey.BANNER_COL, "no").equals("yes")){
            int isPro = new Prefs(WeatherApplication.get()).getPremium();
            if (isPro == 0){
                BannerCollapsibleAds.loadBannerAds(this, viewGroup);
            }else {
                BannerCollapsibleAds.hideBannerLoading(this, true);
            }

        }else {
            try {
                if(BannerAds.getViewRoot() != null){
                    if(BannerAds.getViewRoot().getParent() != null) {
                        ((ViewGroup) BannerAds.getViewRoot().getParent()).removeView(BannerAds.getViewRoot()); // <- fix
                    }
                }
                int isPro = new Prefs(WeatherApplication.get()).getPremium();
                boolean isSub = new Prefs(WeatherApplication.get()).isRemoveAd();
                if (isPro == 1) {
                    return;
                } else if (isSub) {
                    return;
                }

                viewGroup.addView(BannerAds.getViewRoot());
                Log.e("", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
