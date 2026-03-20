package com.example.piceditor.uiFragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.example.piceditor.R
import com.example.piceditor.adapters.GalleryAlbumImageAdapter
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.FragmentGalleryAlbumImageBinding

class GalleryAlbumImageFragment : BaseFragment<FragmentGalleryAlbumImageBinding>() {

    companion object {
        const val ALBUM_IMAGE_EXTRA = "albumImage"
        const val ALBUM_NAME_EXTRA = "albumName"
    }

    private var mImages: ArrayList<String> = ArrayList()
    private var names: String = ""
    private var mListener: OnSelectImageListener? = null

    override fun getLayoutRes() = R.layout.fragment_gallery_album_image
    override fun initView() {}
    override fun initData() {}
    override fun setListener() {}
    override fun setObserver() {}
    override fun getFrame() = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindView(inflater, container, R.layout.fragment_gallery_album_image)

        if (activity is OnSelectImageListener) {
            mListener = activity as OnSelectImageListener
        }

        arguments?.let {

            mImages = it.getStringArrayList(ALBUM_IMAGE_EXTRA) ?: ArrayList()
            names = it.getString(ALBUM_NAME_EXTRA) ?: ""

            binding.gridView.adapter =
                GalleryAlbumImageAdapter(requireActivity(), mImages)

            binding.gridView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    mListener?.onSelectImage(mImages[position])
                }
        }

        return binding.root
    }

    interface OnSelectImageListener {
        fun onSelectImage(str: String)
    }
}
