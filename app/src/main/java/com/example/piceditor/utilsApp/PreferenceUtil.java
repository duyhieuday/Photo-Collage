package com.example.piceditor.utilsApp;

import android.content.Context;
import android.content.SharedPreferences;


public class PreferenceUtil {
    private SharedPreferences IShare = null;
    private static PreferenceUtil mSharedPre;

    public PreferenceUtil(Context context) {
        if (context != null)
            IShare = context.getSharedPreferences("myBase", Context.MODE_PRIVATE);
    }

    public PreferenceUtil(Context context, String fake) {
        if (context != null)
            IShare = context.getSharedPreferences("myBase", Context.MODE_PRIVATE);
    }

    public static PreferenceUtil getInstance(Context context) {
        if (mSharedPre == null) {
            mSharedPre = new PreferenceUtil(context.getApplicationContext(), "");
        }
        return mSharedPre;
    }

    public void setValue(String key, long val) {
        IShare.edit().putLong(key, val).commit();
    }

    public void setValue(String key, String val) {
        IShare.edit().putString(key, val).commit();
    }

    public void setValue(String key, int val) {
        IShare.edit().putInt(key, val).commit();
    }

    public int getValue(String key, int delVal) {
        return IShare.getInt(key, delVal);
    }

    public void setValue(String key, boolean val) {
        IShare.edit().putBoolean(key, val).commit();
    }

    public boolean getValue(String key, boolean delValBoolean) {
        return IShare.getBoolean(key, delValBoolean);
    }

    public long getValueLong(String key, long delVal) {
        return IShare.getLong(key, delVal);
    }

    public String getValue(String key, String delVal) {
        return IShare.getString(key, delVal).trim().toString();
    }

    public void setNotificationCount(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCount(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCount(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void setNotificationCountTimeline(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCountTimeline(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountTimeline(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void removeValue(String key) {
        IShare.edit().remove(key).commit();
    }

    public void setNotificationCountFriend(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCountFriend(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountFriend(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void setNotificationCountFriendRequest(String key, int countNoti) {
        countNoti = (int) getNotificationCountFriendRequest(key, 0) + countNoti;
        IShare.edit().putInt(key, countNoti).commit();
    }

    public void setNotificationCountFriendRequestDefault(String key, int countNoti) {
        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountFriendRequest(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void setNotificationCountAcceptFriend(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCountAcceptFriend(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountAcceptFriend(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void setNotificationCountSetting(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCountSetting(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountSetting(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void setNotificationCountSticker(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCountSticker(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountSticker(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }

    public void setNotificationCountBadge(String key, int countNoti) {
        if (countNoti > 0)
            countNoti = (int) getNotificationCountBadge(key, 0) + countNoti;

        IShare.edit().putInt(key, countNoti).commit();
    }

    public int getNotificationCountBadge(String key, int countNoti) {
        return IShare.getInt(key, countNoti);
    }
}
