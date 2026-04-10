package com.ezt.pdfreader.photoeditor.ui.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.data.PageState
import com.ezt.pdfreader.photoeditor.databinding.DialogPeApplyAllBinding
import com.ezt.pdfreader.photoeditor.databinding.DialogPeDeletePageBinding
import com.ezt.pdfreader.photoeditor.databinding.FragmentPhotoCropperBinding
import com.ezt.pdfreader.photoeditor.ui.activity.PhotoEditorActivity
import com.ezt.pdfreader.photoeditor.ui.adapter.PagePagerAdapter
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorEvent
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorViewModel
import com.mct.doc.scanner.view.PerspectiveImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable

class PhotoCropperFragment : Fragment() {

    companion object {
        private const val ARG_SINGLE_PAGE = "arg_single_page"

        fun newSinglePageInstance(): PhotoCropperFragment {
            return PhotoCropperFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_SINGLE_PAGE, true) }
            }
        }
    }

    /**
     * NO_CROP  – user xoá crop, applyToAll = xoá crop tất cả trang
     * AI       – AI vừa detect, applyToAll = AI crop tất cả trang
     * MANUAL   – user kéo tay hoặc default, applyToAll = copy crop hiện tại sang tất cả trang
     */
    private enum class CropMode { MANUAL, NO_CROP, AI }

    private var _binding: FragmentPhotoCropperBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PhotoEditorViewModel by activityViewModels()
    private lateinit var pagerAdapter: PagePagerAdapter

    private var cropMode = CropMode.MANUAL
    private val isSinglePage: Boolean
        get() = arguments?.getBoolean(ARG_SINGLE_PAGE, false) == true

    // ─────────────────────────────────────────────────────────────

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhotoCropperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViewPager()
        setupPageIndicator()
        setupBottomBar()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnDeletePage.setOnClickListener { viewModel.requestDeleteCurrentPage() }
    }

    private fun setupViewPager() {
        pagerAdapter = PagePagerAdapter()
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        (binding.viewPager.getChildAt(0) as? RecyclerView)?.itemAnimator = null
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (!isSinglePage) viewModel.setCurrentPage(position)
            }
        })
    }

    private fun setupPageIndicator() {
        binding.btnPrevPage.setOnClickListener { viewModel.previousPage() }
        binding.btnNextPage.setOnClickListener { viewModel.nextPage() }
    }

    private fun setupBottomBar() {
        // ── Rotate ───────────────────────────────────────────────
        binding.btnRotate.setOnClickListener {
            withCurrentPvView { pvView ->
                pvView.rotateLeft()
                viewModel.setRotation(pvView.rotationAngle.toInt())
            }
        }

        // ── Flip ─────────────────────────────────────────────────
        binding.btnFlip.setOnClickListener {
            withCurrentPvView { pvView ->
                pvView.toggleFlipX()
                viewModel.setFlipX(pvView.isFlipX)
            }
        }

        // ── No Crop ──────────────────────────────────────────────
        binding.btnNoCrop.setOnClickListener {
            viewModel.setFullCrop()
            setCropMode(CropMode.NO_CROP, showApplyToAll = true)
        }

        // ── AI Crop ──────────────────────────────────────────────
        binding.btnAiCrop.setOnClickListener {
//            viewModel.detectCornersWithAI()
        }

        // ── Apply to All ─────────────────────────────────────────
        binding.btnApplyToAll.setOnClickListener {
            when (cropMode) {
                CropMode.NO_CROP -> showApplyAllDialog(
                    iconRes = R.drawable.ic_pe_thumb_crop_apply_all,
                    titleRes = R.string.pe_apply_no_crop_title,
                    messageRes = R.string.pe_apply_no_crop_message
                ) {
                    viewModel.applyNoCropToAllPages()
                    Toast.makeText(requireContext(), R.string.pe_apply_to_all, Toast.LENGTH_SHORT).show()
                }
                CropMode.AI -> showApplyAllDialog(
                    iconRes = R.drawable.ic_pe_thumb_crop_apply_all,
                    titleRes = R.string.pe_apply_ai_crop_title,
                    messageRes = R.string.pe_apply_ai_crop_message
                ) {
                    viewModel.applyAiCropToAllPages()
                }
                CropMode.MANUAL -> {
                    syncCornersFromView()
                    viewModel.applyCurrentCropToAllPages()
                    Toast.makeText(requireContext(), R.string.pe_apply_to_all, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Next / Done ─────────────────────────────────────────────
        if (isSinglePage) {
            binding.btnNext.setText(R.string.pe_done)
            binding.btnDeletePage.isVisible = false
        }
        binding.btnNext.setOnClickListener {
            syncCornersFromView()
            if (isSinglePage) {
                // Quay lại filter screen, reload ảnh với crop mới
                parentFragmentManager.popBackStack()
            } else {
                (requireActivity() as PhotoEditorActivity).replaceFilter()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Crop mode UI
    // ─────────────────────────────────────────────────────────────

    private fun setCropMode(mode: CropMode, showApplyToAll: Boolean = false) {
        cropMode = mode
        binding.btnApplyToAll.isVisible = !isSinglePage && showApplyToAll && viewModel.pages.value.size > 1
        when (mode) {
            CropMode.NO_CROP -> {
                binding.btnNoCrop.isVisible = true
                binding.btnAiCrop.isVisible = false
                binding.btnApplyToAll.setIconResource(R.drawable.ic_pe_no_crop)
            }
            CropMode.AI -> {
                binding.btnNoCrop.isVisible = true
                binding.btnAiCrop.isVisible = false
                binding.btnApplyToAll.setIconResource(R.drawable.ic_pe_ai_crop)
            }
            CropMode.MANUAL -> {
                binding.btnNoCrop.isVisible = true
                binding.btnAiCrop.isVisible = false
                binding.btnApplyToAll.setIconResource(R.drawable.ic_pe_crop)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Observe
    // ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pages.collect { pages ->
                        val idx = viewModel.currentPageIndex.value
                        val adapterPos = if (isSinglePage) 0 else idx
                        pagerAdapter.setCropModeEnabled(true, adapterPos)
                        if (isSinglePage) {
                            val currentPage = pages.getOrNull(idx)
                            pagerAdapter.submitList(if (currentPage != null) listOf(currentPage) else emptyList())
                        } else {
                            pagerAdapter.submitList(pages)
                        }
                        updatePageIndicator()
                        if (!isSinglePage) binding.btnDeletePage.isVisible = pages.size > 1
                    }
                }
                launch {
                    viewModel.currentPageIndex.collect { index ->
                        if (!isSinglePage) {
                            if (binding.viewPager.currentItem != index)
                                binding.viewPager.setCurrentItem(index, false)
                        }
                        updatePageIndicator()
                        val state = viewModel.pages.value.getOrNull(index) ?: return@collect
                        val initialMode = when {

                            state.corners == null          -> CropMode.NO_CROP
//                            state.cornersWithAI != null    -> CropMode.AI
                            else                           -> CropMode.MANUAL
                        }
                        setCropMode(initialMode)
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        val adapterPos = if (isSinglePage) 0 else viewModel.currentPageIndex.value
                        pagerAdapter.setLoading(loading, adapterPos)
                    }
                }
                launch { viewModel.events.collectLatest { handleEvent(it) } }
            }
        }
    }

    private fun handleEvent(event: PhotoEditorEvent) {
        val idx = viewModel.currentPageIndex.value
        val adapterPos = if (isSinglePage) 0 else idx
        when (event) {
            is PhotoEditorEvent.CornersChanged -> {
                val corners = viewModel.pages.value.getOrNull(idx)?.corners
                pagerAdapter.updateCorners(corners, adapterPos)
                // AI detect thành công → chuyển sang AI mode, hiện apply to all
//                if (corners != null) setCropMode(CropMode.AI, showApplyToAll = true)
            }
//            is PhotoEditorEvent.AiCropAllDone -> {
//                setCropMode(CropMode.AI, showApplyToAll = true)
//                val corners = viewModel.pages.value.getOrNull(idx)?.corners
//                pagerAdapter.updateCorners(corners, adapterPos)
//                Toast.makeText(requireContext(), R.string.pe_apply_to_all, Toast.LENGTH_SHORT).show()
//            }
            is PhotoEditorEvent.ConfirmDeletePage -> showDeleteDialog(event.index)
            is PhotoEditorEvent.PageDeleted ->
                Toast.makeText(requireContext(), R.string.pe_page_deleted, Toast.LENGTH_SHORT).show()
            is PhotoEditorEvent.CannotDeleteLastPage ->
                Toast.makeText(requireContext(), R.string.pe_cannot_delete_last_page, Toast.LENGTH_SHORT).show()
            is PhotoEditorEvent.Error ->
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private fun updatePageIndicator() {
        if (isSinglePage) {
            binding.bottomControls.isVisible = false
            return
        }
        val total = viewModel.pages.value.size
        val currentIndex = viewModel.currentPageIndex.value
        val singlePage = total <= 1
        binding.tvPageIndicator.text = "${currentIndex + 1}/$total"
        binding.bottomControls.alpha = if (singlePage) 0.38f else 1f
        binding.bottomControls.isEnabled = !singlePage
        binding.btnPrevPage.apply {
            isEnabled = !singlePage && currentIndex > 0
            alpha = if (isEnabled) 1f else 0.3f
        }
        binding.btnNextPage.apply {
            isEnabled = !singlePage && currentIndex < total - 1
            alpha = if (isEnabled) 1f else 0.3f
        }
    }

    private fun currentPageState() =
        viewModel.pages.value.getOrNull(viewModel.currentPageIndex.value)

    private inline fun withCurrentPvView(block: (PerspectiveImageView) -> Unit) {
        val rv = binding.viewPager.getChildAt(0) as? RecyclerView ?: return
        val adapterPosition = if (isSinglePage) 0 else viewModel.currentPageIndex.value
        val pv = pagerAdapter.getPerspectiveImageView(rv, adapterPosition) ?: return
        block(pv)
    }

    /**
     * Đọc corners hiện tại từ pvView, scale từ bitmap space → original space và lưu vào ViewModel.
     */
    private fun syncCornersFromView() {
        withCurrentPvView { pvView ->
            val bitmapCorners = pvView.cornersArray ?: return@withCurrentPvView
            val state = currentPageState() ?: return@withCurrentPvView
            val drawable = pvView.drawable ?: return@withCurrentPvView
            val bw = drawable.intrinsicWidth
            val bh = drawable.intrinsicHeight
            if (bw <= 0 || bh <= 0 || state.originalWidth <= 0 || state.originalHeight <= 0) return@withCurrentPvView
            val originalCorners = PageState.scaleCorners(
                bitmapCorners, bw, bh, state.originalWidth, state.originalHeight
            )
            viewModel.updateCorners(originalCorners)
        }
    }

    private fun showApplyAllDialog(iconRes: Int, titleRes: Int, messageRes: Int, onApply: () -> Unit) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val b = DialogPeApplyAllBinding.inflate(layoutInflater)
        b.ivIcon.setImageResource(iconRes)
        b.tvTitle.setText(titleRes)
        b.tvMessage.setText(messageRes)
        dialog.setContentView(b.root)
        b.btnCancel.setOnClickListener { dialog.dismiss() }
        b.btnApply.setOnClickListener { onApply(); dialog.dismiss() }
        dialog.show()
    }

    private fun showDeleteDialog(pageIndex: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val b = DialogPeDeletePageBinding.inflate(layoutInflater)
        dialog.setContentView(b.root)
        b.btnCancel.setOnClickListener { dialog.dismiss() }
        b.btnDelete.setOnClickListener { viewModel.deletePage(pageIndex); dialog.dismiss() }
        dialog.show()
    }
}
