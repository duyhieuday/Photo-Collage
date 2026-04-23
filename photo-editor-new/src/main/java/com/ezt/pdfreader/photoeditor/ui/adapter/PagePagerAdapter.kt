package com.ezt.pdfreader.photoeditor.ui.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.data.FilterType
import com.ezt.pdfreader.photoeditor.data.PageState
import com.ezt.pdfreader.photoeditor.databinding.ItemPePageBinding
import com.ezt.pdfreader.photoeditor.transform.CropTransform
import com.ezt.pdfreader.photoeditor.transform.FilterTransform
import com.ezt.pdfreader.photoeditor.util.BitmapLoader
import com.mct.doc.scanner.view.PerspectiveImageView

class PagePagerAdapter :
    ListAdapter<PageState, PagePagerAdapter.PageViewHolder>(PageDiffCallback()) {

    companion object {
        private const val PAYLOAD_RELOAD    = "payload_reload"
        private const val PAYLOAD_CORNERS   = "payload_corners"
        private const val PAYLOAD_TRANSFORM = "payload_transform"
        private const val PAYLOAD_ADJUST    = "payload_adjust"
        private const val PAYLOAD_LOADING   = "payload_loading"
        private const val PAYLOAD_CROP_MODE = "payload_crop_mode"
    }

    private var cropModeEnabled = false
    private var showLoading = false

    // ✅ Bỏ pendingCorners shared state — mỗi ViewHolder tự đọc từ PageState

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val item = getItem(position)
        if (PAYLOAD_RELOAD in payloads) {
            holder.loadImage(item)
            return
        }
        if (PAYLOAD_TRANSFORM in payloads) holder.applyTransform(item)
        if (PAYLOAD_ADJUST in payloads)    holder.applyAdjust(item)
        // ✅ Corners lấy từ PageState của item, không dùng pendingCorners nữa
        if (PAYLOAD_CORNERS in payloads)   holder.applyCorners(item)
        if (PAYLOAD_LOADING in payloads)   holder.setLoading(showLoading)
        if (PAYLOAD_CROP_MODE in payloads) holder.updateMaskVisibility()
    }

    fun setCropModeEnabled(enabled: Boolean, position: Int) {
        if (cropModeEnabled == enabled) return
        cropModeEnabled = enabled
        if (position in 0 until itemCount) notifyItemChanged(position, PAYLOAD_CROP_MODE)
    }

    fun reloadImage(position: Int) {
        if (position in 0 until itemCount) notifyItemChanged(position, PAYLOAD_RELOAD)
    }

    fun updateTransform(position: Int) {
        if (position in 0 until itemCount) notifyItemChanged(position, PAYLOAD_TRANSFORM)
    }

    // ✅ Update corners cho 1 trang cụ thể — đọc từ PageState
    fun updateCorners(position: Int) {
        if (position in 0 until itemCount) notifyItemChanged(position, PAYLOAD_CORNERS)
    }

    // ✅ Update corners cho TẤT CẢ trang — dùng khi applyNoCropToAll / applyCurrentCropToAll
    fun updateCornersAllPages() {
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PAYLOAD_CORNERS)
    }

    fun updateAdjust(position: Int) {
        if (position in 0 until itemCount) notifyItemChanged(position, PAYLOAD_ADJUST)
    }

    fun setLoading(loading: Boolean, position: Int) {
        showLoading = loading
        if (position in 0 until itemCount) notifyItemChanged(position, PAYLOAD_LOADING)
    }

    fun getPerspectiveImageView(recyclerView: RecyclerView, position: Int): PerspectiveImageView? =
        (recyclerView.findViewHolderForAdapterPosition(position) as? PageViewHolder)?.getPerspectiveImageView()

    // ─────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────

    inner class PageViewHolder(private val binding: ItemPePageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(state: PageState) {
            val color = ContextCompat.getColor(binding.root.context, R.color.pe_accent)
            binding.ivPage.maskStyle = PerspectiveImageView.MaskStyle.createElegantStyle(color)
            binding.ivPage.isMaskMoveable = itemCount == 1
            binding.ivPage.isMaskVisible = cropModeEnabled
            loadImage(state)
        }

        fun getPerspectiveImageView(): PerspectiveImageView = binding.ivPage

        fun updateMaskVisibility() {
            binding.ivPage.isMaskVisible = cropModeEnabled
        }

        fun setLoading(loading: Boolean) {
            binding.progressBar.isVisible = loading
        }

        fun loadImage(state: PageState) {
            binding.ivPage.setImageDrawable(null)
            binding.progressBar.isVisible = true

            var request = BitmapLoader.request(binding.ivPage.context, state.uri)
                .override(BitmapLoader.PREVIEW_SIZE)

            val transforms = mutableListOf<BitmapTransformation>()
            if (!cropModeEnabled && state.corners != null) {
                transforms.add(CropTransform(state.corners!!, state.originalWidth, state.originalHeight))
            }
            if (!cropModeEnabled && state.filterType != FilterType.NONE) {
                transforms.add(FilterTransform(state.filterType, binding.ivPage.context))
            }
            if (transforms.isNotEmpty()) {
                request = request
                    .transform(*transforms.toTypedArray())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
            }

            request.into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.progressBar.isVisible = false
                    binding.ivPage.setImageBitmap(resource)
                    applyAllToPvView(state, resource)
                }
                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.ivPage.setImageDrawable(null)
                }
            })
        }

        // ✅ Đọc corners trực tiếp từ PageState — không phụ thuộc vào pendingCorners nữa
        fun applyCorners(state: PageState) {
            setCorner(scaleToDisplay(state.corners, state))
        }

        fun applyTransform(state: PageState) {
            binding.ivPage.rotationAngle = state.rotation.toFloat()
            binding.ivPage.isFlipX = state.flipX
            binding.ivPage.isFlipY = state.flipY
        }

        fun applyAdjust(state: PageState) {
            binding.ivPage.brightness = state.brightness
            binding.ivPage.contrast = state.contrast
            binding.ivPage.saturation = state.saturation
            binding.ivPage.warmth = state.warmth
        }

        private fun applyAllToPvView(state: PageState, bitmap: Bitmap) {
            binding.ivPage.rotationAngle = state.rotation.toFloat()
            binding.ivPage.isFlipX = state.flipX
            binding.ivPage.isFlipY = state.flipY
            binding.ivPage.brightness = state.brightness
            binding.ivPage.contrast = state.contrast
            binding.ivPage.saturation = state.saturation
            binding.ivPage.warmth = state.warmth
            setCorner(scaleToDisplay(state.corners, state, bitmap.width, bitmap.height))
        }

        private fun setCorner(corners: IntArray?) {
            binding.ivPage.post {
                if (corners != null) binding.ivPage.cornersArray = corners
                else binding.ivPage.setCorners()  // reset về full (no crop)
            }
        }

        private fun scaleToDisplay(corners: IntArray?, state: PageState): IntArray? {
            if (corners == null) return null
            val drawable = binding.ivPage.drawable ?: return corners
            return scaleToDisplay(corners, state, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }

        private fun scaleToDisplay(corners: IntArray?, state: PageState, bw: Int, bh: Int): IntArray? {
            if (corners == null) return null
            return PageState.scaleCorners(corners, state.originalWidth, state.originalHeight, bw, bh)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DiffCallback
    // ─────────────────────────────────────────────────────────────

    private class PageDiffCallback : DiffUtil.ItemCallback<PageState>() {
        override fun areItemsTheSame(oldItem: PageState, newItem: PageState) =
            oldItem.uri == newItem.uri

        override fun areContentsTheSame(oldItem: PageState, newItem: PageState) =
            oldItem.uri == newItem.uri &&
                    oldItem.filterType == newItem.filterType &&
                    oldItem.rotation == newItem.rotation &&
                    oldItem.flipX == newItem.flipX &&
                    oldItem.flipY == newItem.flipY &&
                    oldItem.brightness == newItem.brightness &&
                    oldItem.contrast == newItem.contrast &&
                    oldItem.saturation == newItem.saturation &&
                    oldItem.warmth == newItem.warmth &&
                    nullableArrayEquals(oldItem.corners, newItem.corners) &&
                    nullableArrayEquals(oldItem.cornersWithAI, newItem.cornersWithAI)

        private fun nullableArrayEquals(a: IntArray?, b: IntArray?): Boolean {
            if (a == null && b == null) return true
            if (a == null || b == null) return false
            return a.contentEquals(b)
        }
    }
}