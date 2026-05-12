package com.example.piceditor.sever.ai_remove_bg.token.genart;

import android.annotation.SuppressLint;
import android.provider.Settings;
import android.util.Base64;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.piceditor.WeatherApplication;
import com.example.piceditor.utilsApp.PrefsKot;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class DeviceToken {
    private static final String SECRET_KEY = "YCRLIVHEKCVYMUIGJTPXKYVCTXSHBWDSWZFHLUWEBOLNSQKHUIQVTCMESLBIHMGSNGHEAFTANSTUNCYBVTIBJXSHQWJEFGMVNQOQ";

    public static String getAccessToken() {
        PrefsKot sharedPre = PrefsKot.Companion.getInstance();
        String token = sharedPre.getTokenGenArt();
        if (token.equals("failed")) {
            token = generateAccessToken();
            sharedPre.setTokenGenArt(token);
        } else {
            Device device = new Gson().fromJson(decodePayload(token), Device.class);
            if (device != null) {
                if (device.getExp() <= System.currentTimeMillis()) {
                    token = generateAccessToken();
                    sharedPre.setTokenGenArt(token);
                }
            } else {
                token = generateAccessToken();
                sharedPre.setTokenGenArt(token);
            }
        }
        return token;
    }


    public static String generateAccessToken() {
        DeviceData deviceData = new DeviceData(getDeviceId());

        Device device = new Device();
        device.setDeviceData(deviceData);
        device.setExp(System.currentTimeMillis() + (1000L * 60 * 60 * 6));
        String deviceString = new Gson().toJson(device);

        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            return JWT.create()
                    .withPayload(deviceString)
                    .sign(algorithm);
        } catch (Exception e) {
            return "failed";
        }
    }

    public static String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return "";
        }

        String payloadBase64 = parts[1];
        byte[] payloadBytes = Base64.decode(payloadBase64, Base64.URL_SAFE);
        return new String(payloadBytes, StandardCharsets.UTF_8);
    }

    @SuppressLint("HardwareIds")
    private static String getDeviceId() {
        return Settings.Secure.getString(WeatherApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
