package com.example.piceditor.sever.ai_remove_bg.token.genart;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("data")
    private DeviceData deviceData;
    @SerializedName("exp")
    private Long exp = 0L;

    public Device() {
    }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(DeviceData deviceData) {
        this.deviceData = deviceData;
    }
    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }
}
