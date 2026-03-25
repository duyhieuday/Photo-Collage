package com.example.piceditor.uiFragments

import android.annotation.SuppressLint
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
import com.example.piceditor.adapters.GalleryImageAdapter
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.FragmentGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryFragment : BaseFragment<FragmentGalleryBinding>() {

    private var allImages = ArrayList<String>()
    private var currentImages = ArrayList<String>()

    private lateinit var adapter: GalleryImageAdapter
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
            adapter.updateData(allImages)
            updateTabUI(binding.tabAll)
        }

        binding.tabCamera.setOnClickListener {
            val cameraImages = allImages.filter { it.contains("Camera") }
            adapter.updateData(ArrayList(cameraImages))
            updateTabUI(binding.tabCamera)
        }

        binding.tabDownload.setOnClickListener {
            val downloadImages = allImages.filter { it.contains("Download") }
            adapter.updateData(ArrayList(downloadImages))
            updateTabUI(binding.tabDownload)
        }
    }

    private fun updateTabUI(selected: TextView) {

        val normalColor = "#98A2B3".toColorInt()
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
        lifecycleScope.launch {

            binding.progressBar.visibility = View.VISIBLE

            val images = withContext(Dispatchers.IO) {
                getAllImages()
            }

            binding.progressBar.visibility = View.GONE

            allImages = images
            currentImages = ArrayList(images)

            adapter = GalleryImageAdapter(requireContext(), currentImages) { path ->
                listener?.onSelectImage(path)
            }

            binding.recyclerView.adapter = adapter
        }
    }

    @SuppressLint("Range")
    private fun getAllImages(): ArrayList<String> {

        val list = ArrayList<String>()

        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media.DATA
        )

        val cursor: Cursor? = requireContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val path = it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
                    list.add(path)
                } while (it.moveToNext())
            }
        }

        return list
    }

    interface OnSelectImageListener {
        fun onSelectImage(path: String)
    }
}