package com.example.piceditor.templates_editor

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.example.piceditor.BuildConfig
import com.example.piceditor.WeatherApplication
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ABRC {
    private const val SERVER_URL = "https://gencandroid.com/user/save_data_2.php"
    private const val GEO_PLUGIN_URL = "https://ipwho.is/"

    private var countryName: String = "Unknown"
    private var installTime: String = "Unknown"
    private var lastUpdate: String = "Unknown"
    private var referrer: String = "Unknown"

    fun getCountry() {
        Thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(GEO_PLUGIN_URL).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                val json = JSONObject(body ?: "{}")
                countryName = json.optString("country", "Unknown")
            } catch (e: Exception) {
                countryName = "Unknown"
            }
        }.start()
    }

    fun getPackageInfo(context: Context) {
        val packageManager = context.packageManager
        try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)

            val sdf = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())

            installTime = sdf.format(Date(packageInfo.firstInstallTime))
            lastUpdate = sdf.format(Date(packageInfo.lastUpdateTime))
        } catch (e: Exception) {
            installTime = "Unknown"
            lastUpdate = "Unknown"
        }
    }

    fun check(context: Context) {
        try {
            val referrerClient: InstallReferrerClient =
                InstallReferrerClient.newBuilder(context).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    try {
                        if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            val response: ReferrerDetails = referrerClient.installReferrer
                            referrer = response.installReferrer
//                            referrer = "gclid=CjwKCAjwy7HEBhBJEiwA5hQNoiDpsXjza76lfMEDa48o1DPs-7ZK8N2wNZpgscudmx33LFvqphjt3BoCXWoQAvD_BwE&gbraid=0AAAAA9kspQYkVqLSZHE6s0WXdg2s_cdFN&gad_source=2"

                            val s = PreferenceUtil.getInstance(context)
                                .getValue(Constant.SharePrefKey.INSTALL_REFER, "no");
                            if (s.equals("no")) {
                                PreferenceUtil.getInstance(context)
                                    .setValue(Constant.SharePrefKey.INSTALL_REFER, "yes");

                                if (referrer.contains("gclid") ||
                                    referrer.contains("gad_source") ||
                                    referrer.contains("gbraid") ||
                                    referrer.contains("apps.facebook.com") ||
                                    referrer.contains("apps.instagram.com")
                                ) {
                                    WeatherApplication.trackingEvent("hehehe_true")
//                                    PreferenceUtil.getInstance(context)
//                                        .setValue(Constant.SharePrefKey.HEHE, true)
                                } else {
                                    WeatherApplication.trackingEvent("hehehe_false")
                                }

                                postToServer(context)
                            }
                        } else {
                            WeatherApplication.trackingEvent("hehehe_error")
                        }
                    } catch (e: Exception) {
                        WeatherApplication.trackingEvent("hehehe_error")
                    }

                    if(countryName == "Vietnam"){
//                        PreferenceUtil.getInstance(context)
//                            .setValue(Constant.SharePrefKey.HEHE, true)
                    }


//                    try {
//                        val tm = WeatherApplication.get().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//
//                        val networkCountry = tm.networkCountryIso.uppercase()
//
//                        if(networkCountry == "VN"){
//                            PreferenceUtil.getInstance(context)
//                                .setValue(Constant.SharePrefKey.HEHE, true)
//                        }
//                    } catch (e: Exception) {
//                    }


                }

                override fun onInstallReferrerServiceDisconnected() {
                    WeatherApplication.trackingEvent("hehehe_error")
                }
            })
        } catch (e: Exception) {
            WeatherApplication.trackingEvent("hehehe_error")
        }
    }

    private fun postToServer(context: Context) {
        Thread{
            try {
                val client = OkHttpClient()

                val jsonMediaType = "application/json; charset=utf-8".toMediaType()

                val jsonBody = JSONObject().apply {
                    put("appName", "Photo Collage")
                    put("version", BuildConfig.VERSION_NAME)
                    put("country", countryName)
                    put("deviceName", Build.BRAND + "-" + Build.MODEL + "-" + Build.DEVICE)
                    put("content", referrer)
                    put("installTime", installTime)
                    put("lastUpdate", lastUpdate)
                    put("osVersion", Build.VERSION.SDK_INT)
                }.toString()

                val body = RequestBody.Companion.create(jsonMediaType, jsonBody)

                val request = Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        try {
                            WeatherApplication.trackingEvent(
                                "uauo",
                                "uauouauo",
                                e.message.toString()
                            )
                        }catch (e: Exception){
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            WeatherApplication.trackingEvent(
                                "uauo",
                                "uauouauo",
                                "Success: ${response.code}"
                            )
                        } catch (e: Exception) {
                        }
                    }
                })
            } catch (e: Exception) {
            }
        }.start()
    }
}