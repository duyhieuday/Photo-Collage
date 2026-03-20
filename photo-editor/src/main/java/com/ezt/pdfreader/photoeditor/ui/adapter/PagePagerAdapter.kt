package com.ezt.pdfreader.photoeditor.ui.adapter

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
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
import com.ezt.pdfreader.photoeditor.effect.ImageEffects
import com.ezt.pdfreader.photoeditor.transform.CropTransform
import com.ezt.pdfreader.photoeditor.transform.FilterTransform
import com.ezt.pdfreader.photoeditor.transform.RotateTransform
import com.ezt.pdfreader.photoeditor.util.BitmapLoader
import com.mct.doc.scanner.view.PerspectiveImageView

class PagePagerAdapter :
    ListAdapter<PageState, PagePagerAdapter.PageViewHolder>(PageDiffCallback()) {

    companion object {
        private const val PAYLOAD_ADJUST = "payload_adjust"
        private const val PAYLOAD_CORNERS = "payload_corners"
        private const val PAYLOAD_LOADING = "payload_loading"
        private const val PAYLOAD_ROTATION = "payload_rotation"
        private const val PAYLOAD_CROP_MODE = "payload_crop_mode"
    }

    private var cropModeEnabled = false
    private var pendingAdjustFilter: ColorMatrixColorFilter? = null
    private var pendingCorners: IntArray? = null
    private var showLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPePageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: PageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when {
            payloads.contains(PAYLOAD_ADJUST) -> {
                holder.updateAdjustFilter(pendingAdjustFilter)
            }

            payloads.contains(PAYLOAD_CORNERS) -> {
                holder.updateCorners(pendingCorners, getItem(position))
            }

            payloads.contains(PAYLOAD_LOADING) -> {
                holder.setLoading(showLoading)
            }

            payloads.contains(PAYLOAD_ROTATION) -> {
                holder.loadImage(getItem(position))
            }

            payloads.contains(PAYLOAD_CROP_MODE) -> {
                holder.updateCropMode(getItem(position))
            }

            else -> {
                super.onBindViewHolder(holder, position, payloads)
            }
        }
    }

    fun setCropModeEnabled(enabled: Boolean, position: Int) {
        if (cropModeEnabled != enabled) {
            cropModeEnabled = enabled
            if (position in 0 until itemCount) {
                notifyItemChanged(position, PAYLOAD_CROP_MODE)
            }
        }
    }

    /**
     * Update adjust filter (brightness, contrast, sharpen) for real-time preview
     */
    fun updateAdjustFilter(pageState: PageState?, position: Int) {
        pendingAdjustFilter = pageState?.let {
            ImageEffects.createAdjustFilter(it.brightness, it.contrast, it.sharpen)
        }
        if (position in 0 until itemCount) {
            notifyItemChanged(position, PAYLOAD_ADJUST)
        }
    }

    /**
     * Update corners after AI detection
     */
    fun updateCorners(corners: IntArray?, position: Int) {
        pendingCorners = corners
        if (position in 0 until itemCount) {
            notifyItemChanged(position, PAYLOAD_CORNERS)
        }
    }

    /**
     * Show/hide loading indicator on a specific page
     */
    fun setLoading(loading: Boolean, position: Int) {
        showLoading = loading
        if (position in 0 until itemCount) {
            notifyItemChanged(position, PAYLOAD_LOADING)
        }
    }

    /**
     * Reload image with updated rotation via Glide (background thread)
     */
    fun updateRotation(position: Int) {
        if (position in 0 until itemCount) {
            notifyItemChanged(position, PAYLOAD_ROTATION)
        }
    }

    fun getPerspectiveImageView(recyclerView: RecyclerView, position: Int): PerspectiveImageView? {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as? PageViewHolder
        return viewHolder?.getPerspectiveImageView()
    }

    inner class PageViewHolder(
        private val binding: ItemPePageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pageState: PageState) {
            // Set style
            val color = ContextCompat.getColor(binding.root.context, R.color.pe_accent)
            binding.ivPage.maskStyle = PerspectiveImageView.MaskStyle.createElegantStyle(color)

            // Set mask visibility based on crop mode
            binding.ivPage.isMaskVisible = cropModeEnabled

            // Set color filter
            binding.ivPage.colorFilter = pageState.getColorMatrixColorFilter()

            loadImage(pageState)
        }

        fun getPerspectiveImageView(): PerspectiveImageView = binding.ivPage

        fun updateCropMode(pageState: PageState) {
            binding.ivPage.isMaskVisible = cropModeEnabled
            // Only reload if corners exist (transform chain changes between modes)
            if (pageState.corners != null) {
                loadImage(pageState)
            } else if (cropModeEnabled) {
                // No corners - just show default full corners overlay
                setCorner(null)
            }
        }

        fun updateAdjustFilter(filter: ColorMatrixColorFilter?) {
            binding.ivPage.colorFilter = filter
        }

        fun updateCorners(corners: IntArray?, pageState: PageState) {
            // Scale corners from original to display coordinates
            setCorner(scaleToDisplay(corners, pageState))
        }

        fun setLoading(loading: Boolean) {
            binding.progressBar.isVisible = loading
        }

        fun setCorner(corners: IntArray?) {
            binding.ivPage.post {
                if (corners != null) {
                    binding.ivPage.cornersArray = corners
                } else {
                    binding.ivPage.setCorners()
                }
            }
        }

        /**
         * Scale corners from original coordinate space to displayed bitmap space.
         */
        private fun scaleToDisplay(corners: IntArray?, pageState: PageState): IntArray? {
            if (corners == null) return null
            val drawable = binding.ivPage.drawable ?: return corners
            val displayW = drawable.intrinsicWidth
            val displayH = drawable.intrinsicHeight
            val (origW, origH) = pageState.getEffectiveDimensions()
            return PageState.scaleCorners(corners, origW, origH, displayW, displayH)
        }

        fun loadImage(pageState: PageState) {

            // Clear old image and show loading for full bind
            binding.ivPage.setImageDrawable(null)
            binding.progressBar.isVisible = true

            // Build transforms: Rotate → Crop → Filter (all on background thread)
            val transforms = mutableListOf<BitmapTransformation>()
            if (pageState.rotation != 0) {
                transforms.add(RotateTransform(pageState.rotation))
            }
            if (!cropModeEnabled && pageState.corners != null) {
                val (effectiveW, effectiveH) = pageState.getEffectiveDimensions()
                transforms.add(CropTransform(pageState.corners!!, effectiveW, effectiveH))
            }
            if (pageState.filterType != FilterType.NONE) {
                transforms.add(FilterTransform(pageState.filterType, binding.ivPage.context))
            }

            var request = BitmapLoader.request(binding.ivPage.context, pageState.uri)
                .override(BitmapLoader.PREVIEW_SIZE)

            if (transforms.isNotEmpty()) {
                request = request.transform(*transforms.toTypedArray())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
            }

            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    binding.progressBar.isVisible = false
                    binding.ivPage.setImageBitmap(resource)
                    if (cropModeEnabled) {
                        // Scale corners from original to display coordinates
                        val (origW, origH) = pageState.getEffectiveDimensions()
                        val scaledCorners = pageState.corners?.let {
                            PageState.scaleCorners(
                                it, origW, origH,
                                resource.width, resource.height
                            )
                        }
                        setCorner(scaledCorners)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    binding.ivPage.setImageDrawable(null)
                }
            }
            request.into(target)
        }
    }

    private class PageDiffCallback : DiffUtil.ItemCallback<PageState>() {
        override fun areItemsTheSame(oldItem: PageState, newItem: PageState): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: PageState, newItem: PageState): Boolean {
            // Ignore adjust values (brightness, contrast, sharpen) - they're updated via payload
            // This prevents full rebind when only adjust values change
            return oldItem.uri == newItem.uri &&
                    nullableArrayEquals(oldItem.corners, newItem.corners) &&
                    nullableArrayEquals(oldItem.cornersWithAI, newItem.cornersWithAI) &&
                    oldItem.filterType == newItem.filterType &&
                    oldItem.rotation == newItem.rotation
        }

        private fun nullableArrayEquals(a: IntArray?, b: IntArray?): Boolean {
            if (a == null && b == null) return true
            if (a == null || b == null) return false
            return a.contentEquals(b)
        }
    }
}
