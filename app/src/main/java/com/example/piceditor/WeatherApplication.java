package com.example.piceditor;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustConfig;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.example.piceditor.ads.OpenAdsHelper;
import com.example.piceditor.ads.Prefs;
import com.example.piceditor.utilsApp.Constant;
import com.example.piceditor.utilsApp.LanguageManager;
import com.example.piceditor.utilsApp.PreferenceUtil;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;


public class WeatherApplication extends Application{

    public static final String CHANNEL_ID = "channel_service_example";
    private static SharedPreferences sharedPref;
    private static WeatherApplication instance;
    public static FirebaseAnalytics mFirebaseAnalytics;
    private Prefs prefs;

    public static WeatherApplication getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());

        new OpenAdsHelper().setup(this);

        initRemoteConfig();

        LanguageManager languageManager = new LanguageManager(this);
        languageManager.updateResource("en");

        // Lưu lại để các Activity sau không hỏi lại
        SharedPreferences prefs = getSharedPreferences("signLanguage", MODE_PRIVATE);
        prefs.edit().putString("getSignLanguage", "en").apply();

        createChannelNotification();

        MobileAds.initialize(
                this,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(
                            @NonNull InitializationStatus initializationStatus) {
                    }
                });


        instance = this;

        initAdjust();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

//        prefs = new Prefs(this);
//        prefs.setPremium(1);
        checkSubscription();

    }

    private void createChannelNotification(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Channel Service Example",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null){
                manager.createNotificationChannel(channel);
            }
        }
    }

    public synchronized static WeatherApplication get() {
        return instance;
    }

    //////////////////////
    private void initAdjust() {
        String appToken = "ujai0r3ht9ts";
        String environment = BuildConfig.DEBUG ? AdjustConfig.ENVIRONMENT_SANDBOX : AdjustConfig.ENVIRONMENT_PRODUCTION;
        AdjustConfig config = new AdjustConfig(this, appToken, environment);
        Adjust.onCreate(config);
    }

    public static void initROAS(double revenue, String currency) {
        try {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(instance);
            SharedPreferences.Editor editor = sharedPref.edit();
            double currentImpressionRevenue = revenue/1000000;
            LogTroasFirebaseAdImpression(currentImpressionRevenue, currency); //LTV pingback provides value in micros, so if you are using that directly,
            // make sure to divide by 10^6
            float previousTroasCache = sharedPref.getFloat("TroasCache", 0); //Use App Local storage to store cache of tROAS
            float currentTroasCache = (float) (previousTroasCache + currentImpressionRevenue);
//check whether to trigger  tROAS event
            if (currentTroasCache >= 0.01) {
                LogTroasFirebaseAdRevenueEvent(currentTroasCache,currency);
                editor.putFloat("TroasCache", 0);//reset TroasCache
            } else {
                editor.putFloat("TroasCache", currentTroasCache);
            }
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void LogTroasFirebaseAdRevenueEvent(float tRoasCache, String currency) {
        try {
            Bundle bundle = new Bundle();
            bundle.putDouble(FirebaseAnalytics.Param.VALUE, tRoasCache);//(Required)tROAS event must include Double Value
            bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency);//put in the correct currency
            mFirebaseAnalytics.logEvent("Daily_Ads_Revenue", bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void LogTroasFirebaseAdImpression(double tRoasCache, String currency) {
        try {
            Bundle bundle = new Bundle();
            bundle.putDouble(FirebaseAnalytics.Param.VALUE, tRoasCache);//(Required)tROAS event must include Double Value
            bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency);//put in the correct currency
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static final class AdjustLifecycleCallbacks implements ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
//            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(Activity activity) {
//            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {

        }
    }

    public static void trackingEvent(String event) {
        try {
            Bundle params = new Bundle();
            String s = event.replace(" ", "_");
            Log.e("eventtttt", s);
            mFirebaseAnalytics.logEvent(s, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackingEvent(String event, String key, String value) {
        try {
            try {
                Map<String, String> paramFlurry = new HashMap<String, String>();
                paramFlurry.put(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Bundle params = new Bundle();
                params.putString(key, value);
                mFirebaseAnalytics.logEvent(event, params);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.e("eventttt", event+"-"+key+value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initRemoteConfig() {
        try {
            FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build();
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

            mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener( new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    fetchConfig();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    BillingClient billingClient;
    void checkSubscription(){
        try {
            prefs = new Prefs(this);
            billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener((billingResult, list) -> {}).build();
            final BillingClient finalBillingClient = billingClient;
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {

                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    try {
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                            finalBillingClient.queryPurchasesAsync(
                                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult1, list) -> {
                                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK){
                                            if(list.size()>0){
                                                prefs.setPremium(1); // set 1 to activate premium feature
                                            }else {
                                                prefs.setPremium(0); // set 0 to de-activate premium feature
                                            }
                                        }
                                    });
                        }
                    } catch (Exception e) {
                    }
                }
            });
        } catch (Exception e) {

        }
    }

    private void fetchConfig() {
        try {
            FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            String banner_coll = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.BANNER_COL);
            String test_language = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.TEST_LANGUAGE);
            String native_after_inter = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.NATIVE_AFTER_INTER);
            String test_obd = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.TEST_OBD);
            String inter_splash = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.INTER_SPLASH);
            String native_full_obd = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.NATIVE_FULL_OBD);
            String click_inter = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.CLICK_INTER);
            String load_ob1 = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.LOAD_OB1);
            String hide_mode = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.HIDE_MODE);
            String remove_ads = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.REMOVE_ADS);
            String native_main = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.NATIVE_MAIN);
            String nt_collapse = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.NT_COLLAPSE);
            String inter_time = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.INTER_TIME);
            String first_flow = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.FIRST_FLOW);
            String inter_onb = mFirebaseRemoteConfig.getString(Constant.RemoteConfigKey.INTER_Onb);

            //CHƯA TEST
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.BANNER_COL, banner_coll);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.TEST_LANGUAGE, test_language);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.NATIVE_AFTER_INTER, native_after_inter);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.TEST_OBD, test_obd);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.INTER_SPLASH, inter_splash);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.NATIVE_FULL_OBD, native_full_obd);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.CLICK_INTER, click_inter);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.LOAD_OB1, load_ob1);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.HIDE_MODE, hide_mode);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.REMOVE_ADS, remove_ads);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.NATIVE_MAIN, native_main);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.NT_COLLAPSE, nt_collapse);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.INTER_TIME, inter_time);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.FIRST_FLOW, first_flow);
            PreferenceUtil.getInstance(this).setValue(Constant.SharePrefKey.INTER_Onb, inter_onb);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
