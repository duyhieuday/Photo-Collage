package com.example.piceditor.uiFragments

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.piceditor.R
import com.example.piceditor.adapters.GalleryAlbumAdapter
import com.example.piceditor.adapters.GalleryAlbumRecyclerAdapter
import com.example.piceditor.base.BaseFragment
import com.example.piceditor.databinding.FragmentGalleryAlbumBinding
import com.example.piceditor.model.GalleryAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class GalleryAlbumFragment(context : Context) : BaseFragment<FragmentGalleryAlbumBinding>() {

    private lateinit var mAlbums: ArrayList<GalleryAlbum>
    private lateinit var mAdapter: GalleryAlbumRecyclerAdapter

    override fun getLayoutRes(): Int {
        return R.layout.fragment_gallery_album
    }

    override fun initView() {

        binding.listView.layoutManager =
            GridLayoutManager(requireContext(), 3)

    }

    override fun initData() {

//        loadAlbums()

    }

    override fun setListener() {}

    override fun setObserver() {}

    override fun getFrame(): Int {
        return 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bindView(inflater, container, R.layout.fragment_gallery_album)

        loadAlbums()

        return binding.root
    }

    private fun loadAlbums() {

        lifecycleScope.launch {

            binding.progressBar.visibility = View.VISIBLE

            val albums = withContext(Dispatchers.IO) {
                loadPhotoAlbums()
            }

            binding.progressBar.visibility = View.GONE

            mAlbums = albums

            mAdapter = GalleryAlbumRecyclerAdapter(
                requireContext(),
                mAlbums,
                object : GalleryAlbumAdapter.OnGalleryAlbumClickListener {

                    override fun onGalleryAlbumClick(galleryAlbum: GalleryAlbum?) {

                        val bundle = Bundle()
                        bundle.putStringArrayList(
                            GalleryAlbumImageFragment.ALBUM_IMAGE_EXTRA,
                            galleryAlbum!!.mImageList
                        )

                        bundle.putString(
                            GalleryAlbumImageFragment.ALBUM_NAME_EXTRA,
                            galleryAlbum.mAlbumName
                        )

                        val fragment = GalleryAlbumImageFragment()
                        fragment.arguments = bundle

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.frame_container, fragment)
                            .addToBackStack(null)
                            .commit()

                    }
                })

            binding.listView.adapter = mAdapter

        }

    }

    @SuppressLint("Range")
    private fun loadPhotoAlbums(): ArrayList<GalleryAlbum> {

        val albumMap = LinkedHashMap<Long, GalleryAlbum>()

        val projection = arrayOf(
            "_id",
            "_data",
            "bucket_id",
            "bucket_display_name",
            "datetaken"
        )

        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val cursor: Cursor? = requireContext().contentResolver.query(
            uri,
            projection,
            null,
            null,
            "date_added DESC"
        )

        val albumList = ArrayList<GalleryAlbum>()

        cursor?.use {

            if (it.moveToFirst()) {

                do {

                    val bucketName =
                        it.getString(it.getColumnIndex("bucket_display_name"))

                    val dateTaken =
                        it.getLong(it.getColumnIndex("datetaken"))

                    val imagePath =
                        it.getString(it.getColumnIndex("_data"))

                    val bucketId =
                        it.getLong(it.getColumnIndex("bucket_id"))

                    var album = albumMap[bucketId]

                    if (album == null) {

                        album = GalleryAlbum(bucketId, bucketName)

                        album.mTakenDate =
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(dateTaken)

                        album.mImageList.add(imagePath)

                        albumMap[bucketId] = album

                    } else {

                        album.mImageList.add(imagePath)

                    }

                } while (it.moveToNext())

                albumList.addAll(albumMap.values)

            }

        }

        return albumList

    }

}