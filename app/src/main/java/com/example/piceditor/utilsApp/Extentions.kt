package com.example.piceditor.utilsApp

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import com.example.piceditor.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics

fun Context.deviceId() = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
fun getDeviceName(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.startsWith(manufacturer)) {
        capitalize(model)
    } else {
        capitalize(manufacturer) + " " + model
    }
}


fun capitalize(s: String): String {
    if (s.isEmpty()) {
        return ""
    }
    val first = s[0]
    return if (Character.isUpperCase(first)) {
        s
    } else {
        first.uppercaseChar().toString() + s.substring(1)
    }
}

fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}

fun Context.initROAS(revenue: Double, currency: String) {
    try {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPref.edit()
        val currentImpressionRevenue = revenue / 1000000 //LTV pingback provides value in micros, so if you are using that directly,
        // make sure to divide by 10^6
        val previousTroasCache = sharedPref.getFloat("TroasCache", 0f) //Use App Local storage to store cache of tROAS
        val currentTroasCache = (previousTroasCache + currentImpressionRevenue).toFloat()
        //check whether to trigger  tROAS event
        if (currentTroasCache >= 0.01) {
            logTroasFirebaseAdRevenueEvent(currentTroasCache, currency)
            editor.putFloat("TroasCache", 0f) //reset TroasCache
        } else {
            editor.putFloat("TroasCache", currentTroasCache)
        }
        editor.apply()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun Context.logTroasFirebaseAdRevenueEvent(tRoasCache: Float, currency: String) {
    try {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val bundle = Bundle()
        bundle.putDouble(FirebaseAnalytics.Param.VALUE, tRoasCache.toDouble()) //(Required)tROAS event must include Double Value
        bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency) //put in the correct currency
        firebaseAnalytics.logEvent("Daily_Ads_Revenue", bundle)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun logFlurry(context: Context, name: String, revenue: Double, currency: String) {
    try {
        val currentImpressionRevenue = revenue / 1000000
//        FlurryAgent.logPayment(name!!, name, 1, currentImpressionRevenue, currency!!, "123456789", HashMap())
        logRevenueToAdImpression(context, name, revenue, currency)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun logRevenueToAdImpression(context: Context, name: String, revenue: Double, currency: String) {
    try {
        val currentImpressionRevenue = revenue / 1000000
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, name)
        bundle.putDouble(FirebaseAnalytics.Param.VALUE, currentImpressionRevenue)
        bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency)
        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
}

//fun initROASMax(context: Context, impressionData: MaxAd, name: String) {
//    val revenue = impressionData.revenue // In USD
//    try {
//        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
//
//        val params = Bundle()
//        params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "appLovin")
//        params.putString(FirebaseAnalytics.Param.AD_SOURCE, impressionData.networkName)
//        params.putString(FirebaseAnalytics.Param.AD_FORMAT, impressionData.format.label)
//        params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, name)
//        params.putDouble(FirebaseAnalytics.Param.VALUE, revenue)
//        params.putString(FirebaseAnalytics.Param.CURRENCY, "USD") // All Applovin revenue is sent in USD
//        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
//    } catch (e: java.lang.Exception) {
//        e.printStackTrace()
//    }
//}



fun Context.logEvent(eventName: String, type: String = "", value: String = "") {
    if (BuildConfig.DEBUG) Log.d("===Event", eventName)

    val firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    val bundle = Bundle()
    bundle.putString("event", javaClass.simpleName)
    bundle.putString(type, value)
    firebaseAnalytics.logEvent(eventName, bundle)
}

fun getStatusBarHeight(context: Context): Int {
    return try {
        var result = 0
        @SuppressLint("InternalInsetResource") val resourceId: Int = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
            result
        } else {
            convertDpToPixel(24f, context) as Int
        }
    } catch (e: java.lang.Exception) {
        convertDpToPixel(24f, context) as Int
    }
}

fun convertDpToPixel(dp: Float, context: Context): Float {
    return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}