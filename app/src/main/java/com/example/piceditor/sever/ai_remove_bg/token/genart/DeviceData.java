package com.example.piceditor.sever.ai_remove_bg.token.genart;

import android.os.Build;

import com.google.gson.annotations.SerializedName;

/**
 * @noinspection ALL
 */
public class DeviceData {

    @SerializedName("type")
    private int type;
    @SerializedName("device_id")
    private int deviceId;
    @SerializedName("user_id")
    private int userId;
    @SerializedName("client_id")
    private String clientId = "";
    @SerializedName("name")
    private String name;

    public DeviceData(String clientId) {
        type = 2;
        deviceId = 0;
        userId = 0;
        name = Build.MODEL;
        this.clientId = clientId;
    }
}

