package com.example.piceditor.ads.iap

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.piceditor.R
import com.example.piceditor.ads.Prefs

/**
 * Soft-sell feature-gating dùng chung.
 * - isPremium(): user đã mua Premium / RemoveAd.
 * - showFeatureDialog(): free user chạm tính năng premium → dialog mời lên Premium,
 *   nhưng "Continue" VẪN cho dùng (không hard-block) → giữ trải nghiệm free.
 */
object PremiumUpsell {

    fun isPremium(context: Context): Boolean {
        val prefs = Prefs(context)
        return prefs.getPremium() == 1 || prefs.isRemoveAd
    }

    fun showFeatureDialog(activity: Activity, onContinue: () -> Unit) {
        try {
            val dialog = Dialog(activity)
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium_upsell, null)
            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.86f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(true)
            view.findViewById<View>(R.id.btnGoPremium).setOnClickListener {
                dialog.dismiss()
                activity.startActivity(Intent(activity, PremiumActivity::class.java))
            }
            view.findViewById<View>(R.id.btnContinueFree).setOnClickListener {
                dialog.dismiss()
                onContinue()
            }
            if (!activity.isFinishing) dialog.show()
        } catch (e: Exception) {
            // Lỗi gì cũng không chặn người dùng — cứ cho tiếp tục
            e.printStackTrace()
            onContinue()
        }
    }
}
