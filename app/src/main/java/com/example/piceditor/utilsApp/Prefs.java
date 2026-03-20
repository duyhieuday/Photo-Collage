package com.example.piceditor.utilsApp;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.StringDef;

import com.example.piceditor.WeatherApplication;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Prefs {

    private Prefs() {
        //no instance
    }

    @StringDef({
            Key.PREMIUM,
            Key.REMOTE_VERSION,
            Key.REMOTE_FORCE_UPDATE,
            Key.REMOTE_FORCE_PREMIUM_VERSION,
            Key.REMOTE_ADS_INTERVAL_APP_OPEN,
            Key.REMOTE_ADS_INTERVAL_INTERSTITIAL,
            Key.REMOTE_ADS_INTERVAL_INTERSTITIAL_FAST,
            Key.REMOTE_ADS_NT_FULL_SCREEN_CLICK_DISMISS_COUNTDOWN,
            Key.REMOTE_HIDE_IT_FAST,
            Key.REMOTE_HIDE_NT_FULL_SCREEN,
            Key.REMOTE_HIDE_NT_ONBOARDING,
            Key.REMOTE_HIDE_NT_LANGUAGE,
            Key.REMOTE_HIDE_NT_TAB_HOME,
            Key.REMOTE_HIDE_NT_TAB_GUIDE,
            Key.REMOTE_HIDE_NT_APPLY_FONT_SPEC,
            Key.REMOTE_HIDE_NT_CUSTOM_FONT_SPEC,
            Key.LANGUAGE,
            Key.LANGUAGE_NAME,
            Key.KEY_LANGUAGE,
            Key.SIGN_LANGUAGE,
            Key.FIRST_ONBOARDING,
            Key.IMAGE_SUCCESS,
            Key.TEST_OBD,
            Key.FLAG,
            Key.ADD_DATA,
            Key.MUSIC,
            Key.FIRST_OPEN,
            Key.REMOTE_OPEN_BANNER,
            Key.REMOTE_HIDE_NATIVE_MAIN,
            Key.REMOTE_INTER_SPLASH,
            Key.REMOTE_NATIVE_LANGUAGE,
            Key.REMOTE_NATIVE_FULL_OBD,
    })
    public @interface Key {
        String PREMIUM = "premium";
        String REMOTE_VERSION = "rm_version";
        String REMOTE_FORCE_UPDATE = "rm_force_update";
        String REMOTE_FORCE_PREMIUM_VERSION = "rm_force_premium_version";
        String REMOTE_ADS_INTERVAL_APP_OPEN = "rm_ads_interval_app_open";
        String REMOTE_ADS_INTERVAL_INTERSTITIAL = "rm_ads_interval_interstitial";
        String REMOTE_ADS_INTERVAL_INTERSTITIAL_FAST = "rm_ads_interval_interstitial_fast";
        String REMOTE_ADS_NT_FULL_SCREEN_CLICK_DISMISS_COUNTDOWN = "rm_ads_nt_full_screen_click_dismiss_countdown";
        String REMOTE_HIDE_IT_FAST = "rm_hit_fast";
        String REMOTE_HIDE_NT_FULL_SCREEN = "rm_hnt_full_screen";
        String REMOTE_HIDE_NT_ONBOARDING = "rm_hnt_onboarding";
        String REMOTE_HIDE_NT_LANGUAGE = "rm_hnt_language";
        String REMOTE_HIDE_NT_TAB_HOME = "rm_hnt_tab_home";
        String REMOTE_HIDE_NT_TAB_GUIDE = "rm_hnt_tab_guide";
        String REMOTE_HIDE_NT_APPLY_FONT_SPEC = "rm_hnt_apply_font_spec";
        String REMOTE_HIDE_NT_CUSTOM_FONT_SPEC = "rm_hnt_custom_font_spec";
        String LANGUAGE = "LANGUAGE";
        String LANGUAGE_NAME = "LANGUAGE_NAME";
        String SIGN_LANGUAGE = "SIGN_LANGUAGE";
        String KEY_LANGUAGE = "KEY_LANGUAGE";
        String FIRST_ONBOARDING = "FIRST_ONBOARDING";
        String IMAGE_SUCCESS = "IMAGE_SUCCESS";
        String TEST_OBD = "rm_test_obd";
        String FLAG = "FLAG";
        String ADD_DATA = "ADD_DATA";
        String MUSIC = "MUSIC";
        String FIRST_OPEN = "FIRST_OPEN";
        String REMOTE_OPEN_BANNER = "rm_open_banner";
        String REMOTE_HIDE_NATIVE_MAIN = "rm_hide_native_main";
        String REMOTE_INTER_SPLASH = "rm_inter_splash";
        String REMOTE_NATIVE_LANGUAGE = "rm_native_language";
        String REMOTE_NATIVE_FULL_OBD = "rm_native_full_obd";
    }

    public static boolean getBoolean(@Key String key) {
        return prefs().getBoolean(key, false);
    }

    public static boolean getBoolean(@Key String key, boolean defaultValue) {
        return prefs().getBoolean(key, defaultValue);
    }

    public static void putBoolean(@Key String key, boolean value) {
        prefsEditor().putBoolean(key, value).apply();
    }

    public static int getInt(@Key String key) {
        return prefs().getInt(key, 0);
    }

    public static int getInt(@Key String key, int defaultValue) {
        return prefs().getInt(key, defaultValue);
    }

    public static void putInt(@Key String key, int value) {
        prefsEditor().putInt(key, value).apply();
    }

    public static long getLong(@Key String key) {
        return prefs().getLong(key, 0);
    }

    public static long getLong(@Key String key, long defaultValue) {
        return prefs().getLong(key, defaultValue);
    }

    public static void putLong(@Key String key, long value) {
        prefsEditor().putLong(key, value).apply();
    }

    public static String getString(@Key String key) {
        return prefs().getString(key, "");
    }

    public static String getString(@Key String key, String defaultValue) {
        return prefs().getString(key, defaultValue);
    }

    public static void putString(@Key String key, String value) {
        prefsEditor().putString(key, value).apply();
    }

    public static Set<String> getStringSet(@Key String key) {
        return prefs().getStringSet(key, new HashSet<>());
    }

    public static Set<String> getStringSet(@Key String key, Set<String> defaultValue) {
        return prefs().getStringSet(key, defaultValue);
    }

    public static void putStringSet(@Key String key, Set<String> value) {
        prefsEditor().putStringSet(key, value).apply();
    }

    public static Map<String, ?> getAll() {
        return prefs().getAll();
    }

    public static void remove(String key) {
        prefsEditor().remove(key).apply();
    }

    public static void registerListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs().registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        prefs().unregisterOnSharedPreferenceChangeListener(listener);
    }

    private static SharedPreferences prefs;
    private static SharedPreferences.Editor prefsEditor;

    public static SharedPreferences prefs() {
        if (prefs == null) {
            prefs = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE);
        }
        return prefs;
    }

    public static SharedPreferences.Editor prefsEditor() {
        if (prefsEditor == null) {
            prefsEditor = prefs().edit();
        }
        return prefsEditor;
    }

    private static Context getContext() {
        return WeatherApplication.getInstance();
    }

}

