package com.ezt.pdfreader.photoeditor.ui.adapter

import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.util.BitmapLoader
import com.ezt.pdfreader.photoeditor.data.FilterType
import com.ezt.pdfreader.photoeditor.databinding.ItemPeFilterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterAdapter(
    private val onFilterSelected: (FilterType) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    private val filters = FilterType.entries.toList()
    private var selectedPosition = 0

    private val thumbnailCache = mutableMapOf<FilterType, Bitmap>()
    private var sourceUri: Uri? = null
    private var sourceBitmap: Bitmap? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemPeFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FilterViewHolder(binding)
    }

    override fun getItemCount(): Int = filters.size

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(filters[position], position == selectedPosition)
    }

    fun setSelectedFilter(filterType: FilterType) {
        val newPosition = filters.indexOf(filterType)
        if (newPosition != -1 && newPosition != selectedPosition) {
            val oldPosition = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(newPosition)
        }
    }

    fun setSourceImage(uri: Uri) {
        if (uri == sourceUri) return
        sourceUri = uri
        sourceBitmap = null
        thumbnailCache.clear()
        notifyItemRangeChanged(0, itemCount)
    }

    fun clearCache() {
        thumbnailCache.clear()
        sourceBitmap = null
    }

    inner class FilterViewHolder(
        private val binding: ItemPeFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var thumbnailJob: Job? = null

        init {
            binding.cardFilter.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position != selectedPosition) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(position)
                    onFilterSelected(filters[position])
                }
            }
        }

        fun bind(filterType: FilterType, isSelected: Boolean) {
            binding.tvFilterName.text = filterType.displayName

            // Premium crown icon
//            binding.ivCrown.isVisible = filterType.isPremium

            // Selection state
            binding.cardFilter.strokeColor = binding.root.context.getColor(
                if (isSelected) R.color.pe_accent else R.color.pe_filter_border
            )
            binding.cardFilter.strokeWidth = if (isSelected) 2f.dpToPx() else 1f.dpToPx()

            // Load thumbnail preview
            if (filterType == FilterType.NONE) {
                loadNoneThumbnail()
            } else {
                loadFilteredThumbnail(filterType)
            }
        }

        private fun loadNoneThumbnail() {
            val uri = sourceUri ?: return
            BitmapLoader.request(binding.ivFilterPreview.context, uri)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .override(THUMBNAIL_SIZE)
                .into(binding.ivFilterPreview)
        }

        private fun loadFilteredThumbnail(filterType: FilterType) {
            thumbnailJob?.cancel()

            // Check cache
            val cached = thumbnailCache[filterType]
            if (cached != null) {
                binding.ivFilterPreview.setImageBitmap(cached)
                return
            }

            // Clear previous
            binding.ivFilterPreview.setImageDrawable(null)

            thumbnailJob = CoroutineScope(Dispatchers.IO).launch {
                val bitmap = getOrLoadSourceBitmap() ?: return@launch

                val gpuFilter = filterType.createGPUFilter(binding.root.context.applicationContext)
                gpuFilter.isThumbnail = true
                gpuFilter.setBitmap(bitmap)
                val result = gpuFilter.getRawBitmap() ?: return@launch

                thumbnailCache[filterType] = result

                withContext(Dispatchers.Main) {
                    // Verify this ViewHolder still shows the same filter
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION && filters[pos] == filterType) {
                        binding.ivFilterPreview.setImageBitmap(result)
                    }
                }
            }
        }

        private fun getOrLoadSourceBitmap(): Bitmap? {
            sourceBitmap?.let { return it }
            val uri = sourceUri ?: return null
            val context = binding.root.context.applicationContext
            val bitmap = BitmapLoader.request(context, uri)
                .override(THUMBNAIL_SIZE)
                .submit()
                .get()
            sourceBitmap = bitmap
            return bitmap
        }
    }

    private fun Float.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    companion object {
        private const val THUMBNAIL_SIZE = 200
    }
}
