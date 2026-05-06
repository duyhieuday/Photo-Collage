package com.example.piceditor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import com.example.piceditor.ads.BannerAds
import com.example.piceditor.ads.BannerCollapsibleAds
import com.example.piceditor.ads.Prefs
import com.example.piceditor.utilsApp.Constant
import com.example.piceditor.utilsApp.PreferenceUtil
import com.ezt.pdfreader.photoeditor.data.PageInfo
import com.ezt.pdfreader.photoeditor.ui.activity.PhotoEditorActivity

class PhotoEditorWithBannerActivity : PhotoEditorActivity(){

    companion object {
        fun createIntent(context: Context, pages: List<PageInfo>): Intent {
            return Intent(context, PhotoEditorWithBannerActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_PAGES, ArrayList(pages))
            }
        }

        fun getResultPages(data: Intent?): List<Uri>? {
            return PhotoEditorActivity.getResultPages(data)
        }

        fun clearCache(context: Context) {
            PhotoEditorActivity.clearCache(context)
        }
    }

    override fun initBannerAds(container: ViewGroup) {
        // Premium → ẩn banner
        val prefs = Prefs(WeatherApplication.get())
        if (prefs.premium == 1 || prefs.isRemoveAd) {
            container.visibility = View.GONE
            return
        }

        val useCollapsible = PreferenceUtil.getInstance(this)
            .getValue(Constant.SharePrefKey.BANNER_COL, "no") == "yes"

        if (useCollapsible) {
            // Banner collapsible: load trực tiếp vào container
            BannerCollapsibleAds.loadBannerAds(this, container)
        } else {
            // Banner thường: lấy viewRoot đã pre-init
            try {
                val view = BannerAds.getViewRoot()
                if (view != null) {
                    (view.parent as? ViewGroup)?.removeView(view)
                    container.addView(view)
                } else {
                    // Fallback nếu chưa init từ trước
                    BannerAds.initBannerAds(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun isPremium(): Boolean {
        val prefs = Prefs(WeatherApplication.get())
        return prefs.premium == 1 || prefs.isRemoveAd
    }

    override fun openPaywall() {

    }
}