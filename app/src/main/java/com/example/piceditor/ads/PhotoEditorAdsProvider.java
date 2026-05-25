package com.example.piceditor.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.ezt.pdfreader.photoeditor.util.AdsProvider;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class PhotoEditorAdsProvider implements AdsProvider {
    @Override public void showInterAds(@NonNull Activity activity, Function0<Unit> onComplete) {
        InterAds.showAdsBreak(activity, onComplete::invoke);
    }
}
