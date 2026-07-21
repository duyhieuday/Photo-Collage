package com.example.piceditor.uiFragments

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.piceditor.R
import com.example.piceditor.SelectImageActivity
import com.example.piceditor.adapters.GalleryImageAdapter
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.FragmentGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment : BaseFragment<FragmentGalleryBinding>() {

    private var allImages = ArrayList<String>()
    private var currentImages = ArrayList<String>()

    // Nullable: user có thể bấm tab TRƯỚC khi load ảnh xong (setListener chạy trước initData)
    private var adapter: GalleryImageAdapter? = null
    private var listener: OnSelectImageListener? = null

    override fun getLayoutRes() = R.layout.fragment_gallery

    override fun initView() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    override fun initData() {
        if (activity is OnSelectImageListener) {
            listener = activity as OnSelectImageListener
        }
        loadImages()
    }

    override fun setListener() {
        binding.tabAll.setOnClickListener {
            adapter?.updateData(allImages)
            updateTabUI(binding.tabAll)
        }

        binding.tabCamera.setOnClickListener {
            val cameraImages = allImages.filter { it.contains("Camera") }
            adapter?.updateData(ArrayList(cameraImages))
            updateTabUI(binding.tabCamera)
        }

        binding.tabDownload.setOnClickListener {
            val downloadImages = allImages.filter { it.contains("Pictures") }
            adapter?.updateData(ArrayList(downloadImages))
            updateTabUI(binding.tabDownload)
        }
    }

    private fun updateTabUI(selected: TextView) {
        val normalColor   = "#98A2B3".toColorInt()
        val selectedColor = "#101828".toColorInt()
        binding.tabAll.setTextColor(normalColor)
        binding.tabCamera.setTextColor(normalColor)
        binding.tabDownload.setTextColor(normalColor)
        selected.setTextColor(selectedColor)
    }

    override fun setObserver() {}
    override fun getFrame() = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindView(inflater, container, R.layout.fragment_gallery)
        return binding.root
    }

    private fun loadImages() {
        // Lấy resolver NGAY trên main thread khi fragment còn attach — query chạy ở IO có thể
        // kết thúc sau khi user back ra, lúc đó requireContext() sẽ ném IllegalStateException.
        val resolver = context?.applicationContext?.contentResolver ?: return

        // viewLifecycleOwner: coroutine bị huỷ khi view destroy → không đụng binding chết.
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            val images = withContext(Dispatchers.IO) { getAllImages(resolver) }

            // Fragment đã detach trong lúc load → bỏ qua, không dựng adapter nữa.
            val ctx = context ?: return@launch
            if (!isAdded) return@launch

            binding.progressBar.visibility = View.GONE

            allImages    = images
            currentImages = ArrayList(images)

            val newAdapter = GalleryImageAdapter(ctx, currentImages) { path ->
                listener?.onSelectImage(path)
            }
            adapter = newAdapter

            binding.recyclerView.adapter = newAdapter

            // ✅ Pass adapter lên SelectImageActivity để sync badge khi xóa
            (activity as? SelectImageActivity)?.setGalleryAdapter(newAdapter)
        }
    }

    @SuppressLint("Range")
    private fun getAllImages(resolver: ContentResolver): ArrayList<String> {
        val list = ArrayList<String>()
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = runCatching {
            resolver.query(
                uri, projection, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
            )
        }.getOrNull()
        cursor?.use {
            val dataCol = it.getColumnIndex(MediaStore.Images.Media.DATA)
            if (dataCol >= 0 && it.moveToFirst()) {
                do {
                    it.getString(dataCol)?.let { path -> list.add(path) }
                } while (it.moveToNext())
            }
        }
        return list
    }

    interface OnSelectImageListener {
        fun onSelectImage(path: String)
    }
}