package com.example.piceditor.ads.iap

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.concurrent.atomic.AtomicBoolean
import com.example.piceditor.R
import com.example.piceditor.ads.Prefs
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil

/**
 * Soft-sell feature-gating dùng chung.
 * - isPremium(): user đã mua Premium / RemoveAd.
 * - showFeatureDialog(): free user chạm tính năng premium → dialog mời lên Premium,
 *   nhưng "Continue" VẪN cho dùng (không hard-block) → giữ trải nghiệm free.
 */
object PremiumUpsell {

    /**
     * Sau khi bấm "Continue" ở feature-gate dialog, onContinue thường gọi InterAds.showAdsBreak
     * → interstitial. Nếu KHÔNG chặn, khi đóng inter sẽ bắn tiếp dialog "Remove ads forever?"
     * (cùng layout) → user tưởng "vẫn dialog cũ", phải Continue lần nữa.
     * Cờ này báo InterAds BỎ QUA remove-ads-upsell cho ĐÚNG 1 lần inter kế tiếp (tránh chồng 2 upsell).
     */
    @JvmField
    var suppressRemoveAdsOnce: Boolean = false

    /**
     * IAP có được BẬT không (cờ tổng HEHE, set từ remote config).
     * HEHE=false → app KHÔNG có gì liên quan IAP: không paywall/crown/badge/gate/watermark.
     */
    @JvmStatic
    fun isIapEnabled(context: Context): Boolean =
        PreferenceUtil.getInstance(context).getValue(Constant.SharePrefKey.HEHE, false)

    fun isPremium(context: Context): Boolean {
        // IAP tắt → coi như đã "premium" (mở khoá mọi tính năng, KHÔNG hiện badge/dialog gate).
        if (!isIapEnabled(context)) return true
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
                // onContinue có thể mở interstitial → chặn remove-ads-upsell chồng lên sau khi đóng inter.
                suppressRemoveAdsOnce = true
                dialog.dismiss()
                onContinue()
            }
            if (!activity.isFinishing) dialog.show() else onContinue()
        } catch (e: Exception) {
            // Lỗi gì cũng không chặn người dùng — cứ cho tiếp tục
            e.printStackTrace()
            onContinue()
        }
    }

    /**
     * Prompt "bỏ quảng cáo" sau khi ĐÓNG interstitial (skip-ads → pay).
     * Java-friendly (Runnable). Dù user đóng kiểu gì — Go Premium / Continue / back / tap ngoài —
     * [onProceed] CHẮC CHẮN chạy đúng 1 LẦN để luồng điều hướng sau ad không bị kẹt.
     */
    @JvmStatic
    fun showRemoveAdsDialog(activity: Activity, onProceed: Runnable) {
        // [onProceed] chạy đúng 1 LẦN dù đi nhánh nào (show / lỗi / activity finishing).
        val proceeded = AtomicBoolean(false)
        val proceedOnce = Runnable { if (proceeded.compareAndSet(false, true)) onProceed.run() }
        // Hoãn 1 nhịp: interstitial vừa đóng, window activity gọi thường CHƯA resume xong →
        // show ngay dễ dính BadToken. Post lên main looper + delay nhỏ cho chắc.
        Handler(Looper.getMainLooper()).postDelayed({
            if (activity.isFinishing || activity.isDestroyed) { proceedOnce.run(); return@postDelayed }
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
                // Đổi tiêu đề sang hướng "bỏ quảng cáo" (các bullet đã bao gồm ad-free/no-watermark)
                view.findViewById<TextView?>(R.id.tvUpsellTitle)?.setText(R.string.iap_remove_ads_title)
                // Đóng kiểu nào (Go Premium / Continue / back / ngoài) cũng proceed đúng 1 lần
                dialog.setOnDismissListener { proceedOnce.run() }
                view.findViewById<View>(R.id.btnGoPremium).setOnClickListener {
                    activity.startActivity(Intent(activity, PremiumActivity::class.java))
                    dialog.dismiss()
                }
                view.findViewById<View>(R.id.btnContinueFree).setOnClickListener { dialog.dismiss() }
                dialog.show()
                Log.d(TAG, "RemoveAds upsell shown")
            } catch (e: Exception) {
                Log.e(TAG, "RemoveAds upsell show failed", e)
                proceedOnce.run()
            }
        }, 500)
    }

    private const val TAG = "PremiumUpsell"
}
