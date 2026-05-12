package com.example.piceditor.utilsApp

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.piceditor.R
import com.example.piceditor.WeatherApplication

class PrefsKot private constructor() {
    private val context = WeatherApplication.get()
    private val prefs: SharedPreferences =
        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: PrefsKot? = null

        fun getInstance(): PrefsKot {
            return instance ?: synchronized(this) {
                instance ?: PrefsKot().also { instance = it }
            }
        }
    }

    //MARK: String
    fun setValue(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    fun getValue(key: String, default: String): String {
        return prefs.getString(key, default)?.trim().orEmpty()
    }

    //MARK: Int
    fun setValue(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    fun getValue(key: String, default: Int): Int {
        return prefs.getInt(key, default)
    }

    //MARK: Long
    fun setValue(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    fun getValue(key: String, default: Long): Long {
        return prefs.getLong(key, default)
    }

    //MARK: Boolean
    fun setValue(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    fun getValue(key: String, default: Boolean): Boolean {
        return prefs.getBoolean(key, default)
    }

    //MARK: Float
    fun setValue(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    fun getValue(key: String, default: Float): Float {
        return prefs.getFloat(key, default)
    }

    //MARK: Remove Value
    fun removeValue(key: String) {
        prefs.edit { remove(key) }
    }

    fun setVipActive(isActive: Boolean) {
        prefs.edit { putBoolean("isVipActive", isActive) }
    }

    fun isVipActive(): Boolean {
        return prefs.getBoolean("isVipActive", false)
    }

    fun setTokenGenArt(token: String) {
        setValue("tokenGenArt", token)
    }

    fun setTokenChatBot(token: String) {
        setValue("tokenChatbot", token)
    }

    fun getTokenGenArt(): String {
        return getValue("tokenGenArt", "failed")
    }

    fun getTokenChatBot(): String {
        return getValue("tokenChatbot", "failed")
    }

}