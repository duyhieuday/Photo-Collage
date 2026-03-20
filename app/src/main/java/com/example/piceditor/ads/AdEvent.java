package com.example.piceditor.ads;

public class AdEvent {
    public final boolean isSuccess;
    public final String message;

    public AdEvent(boolean isSuccess, String message) {
        this.isSuccess = isSuccess;
        this.message = message;
    }
}
