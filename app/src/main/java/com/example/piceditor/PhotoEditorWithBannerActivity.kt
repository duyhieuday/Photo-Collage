package com.example.piceditor

import android.content.Context
import android.content.Intent
import android.net.Uri
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

    override fun initBannerAds() {

    }

    override fun isPremium(): Boolean {
        return false
    }

    override fun openPaywall() {

    }
}