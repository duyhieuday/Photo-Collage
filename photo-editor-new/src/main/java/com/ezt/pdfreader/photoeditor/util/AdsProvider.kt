package com.ezt.pdfreader.photoeditor.util

import android.app.Activity

interface AdsProvider {
    fun showInterAds(activity: Activity, onComplete: () -> Unit)
}

object PhotoEditorAds {
    var provider: AdsProvider? = null
}