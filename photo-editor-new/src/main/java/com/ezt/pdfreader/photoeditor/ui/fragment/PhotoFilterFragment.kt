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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.data.PageState
import com.ezt.pdfreader.photoeditor.databinding.DialogPeApplyAllBinding
import com.ezt.pdfreader.photoeditor.databinding.DialogPeDeletePageBinding
import com.ezt.pdfreader.photoeditor.databinding.FragmentPhotoFilterBinding
import com.ezt.pdfreader.photoeditor.ui.activity.PhotoEditorActivity
import com.ezt.pdfreader.photoeditor.ui.adapter.FilterAdapter
import com.ezt.pdfreader.photoeditor.ui.adapter.PagePagerAdapter
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorEvent
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorViewModel
import com.mct.doc.scanner.view.PerspectiveImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable

class PhotoFilterFragment : Fragment() {

    private enum class AdjustMode { CONTRAST, BRIGHTNESS, SATURATION, WARMTH }

    private var _binding: FragmentPhotoFilterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PhotoEditorViewModel by activityViewModels()
    private lateinit var pagerAdapter: PagePagerAdapter
    private lateinit var filterAdapter: FilterAdapter

    private var currentAdjustMode = AdjustMode.BRIGHTNESS
    private var isAdjustPanelVisible = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhotoFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViewPager()
        setupPageIndicator()
        setupFilterBar()
        setupAdjustPanel()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        filterAdapter.clearCache()
        _binding = null
    }

    // ─────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnCropPage.setOnClickListener {
            (requireActivity() as PhotoEditorActivity).openCropForCurrentPage()
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
                viewModel.setCurrentPage(position)
                syncFilterAdapterForPage(position)
                binding.btnFilterApplyToAll.isVisible = false
            }
        })
    }

    private fun setupPageIndicator() {
        binding.btnPrevPage.setOnClickListener { viewModel.previousPage() }
        binding.btnNextPage.setOnClickListener { viewModel.nextPage() }
    }

    private fun setupFilterBar() {
        binding.bottomBar.isVisible = true
        binding.filterPanel.isVisible = false

        filterAdapter = FilterAdapter { filterType ->
            viewModel.applyFilter(filterType)
            pagerAdapter.reloadImage(viewModel.currentPageIndex.value)
            binding.btnFilterApplyToAll.isVisible = viewModel.pages.value.size > 1
        }
        binding.rvFilters.apply {
            adapter = filterAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
        }

        binding.btnRotate.setOnClickListener {
            withCurrentPvView { pvView ->
                pvView.rotateLeft()
                viewModel.setRotation(pvView.rotationAngle.toInt())
            }
        }
        binding.btnFlip.setOnClickListener {
            withCurrentPvView { pvView ->
                pvView.toggleFlipX()
                viewModel.setFlipX(pvView.isFlipX)
            }
        }
        binding.btnAdjust.setOnClickListener { showAdjustPanel() }
        binding.btnFilterApplyToAll.setOnClickListener {
            showApplyFilterDialog {
                viewModel.applyCurrentFilterToAllPages()
                // Reload all visible pages
                for (i in 0 until viewModel.pages.value.size) {
                    pagerAdapter.reloadImage(i)
                }
                Toast.makeText(requireContext(), R.string.pe_apply_to_all, Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnNext.setOnClickListener {
            val activity = requireActivity() as PhotoEditorActivity
            // Check premium cho tất cả các trang có filter premium
//            val hasPremiumFilter = viewModel.pages.value.any { it.filterType.isPremium }
//            if (hasPremiumFilter && !activity.isPremium()) {
//                activity.openPaywall()
//                return@setOnClickListener
//            }
            activity.saveAndFinish()
        }
    }

    private fun setupAdjustPanel() {
        binding.btnContrast.setOnClickListener   { setAdjustMode(AdjustMode.CONTRAST) }
        binding.btnBrightness.setOnClickListener { setAdjustMode(AdjustMode.BRIGHTNESS) }
        binding.btnSaturation.setOnClickListener { setAdjustMode(AdjustMode.SATURATION) }
        binding.btnWarmth.setOnClickListener     { setAdjustMode(AdjustMode.WARMTH) }

        // Slider kéo → PerspectiveImageView cập nhật real-time (không qua ViewModel)
        binding.sliderAdjust.addOnChangeListener { _, value, fromUser ->
            updateAdjustValueText(value.toInt())
            if (!fromUser) return@addOnChangeListener
            withCurrentPvView { pv ->
                when (currentAdjustMode) {
                    AdjustMode.BRIGHTNESS -> pv.setBrightness(sliderToPv(value))
                    AdjustMode.CONTRAST   -> pv.setContrast(sliderToPv(value))
                    AdjustMode.SATURATION -> pv.setSaturation(sliderToPv(value))
                    AdjustMode.WARMTH     -> pv.setWarmth(sliderToWarmth(value))
                }
            }
        }

        // Cancel → khôi phục lại giá trị đã lưu trong ViewModel
        binding.btnCancel.setOnClickListener {
            val state = viewModel.pages.value.getOrNull(viewModel.currentPageIndex.value)
            if (state != null) withCurrentPvView { pv -> restoreAdjust(pv, state) }
            hideAdjustPanel()
        }

        // Submit → persist giá trị hiện tại của pvView vào ViewModel
        binding.btnSubmit.setOnClickListener {
            syncAdjustToViewModel()
            if (binding.btnApplyToAll.isChecked) {
                viewModel.applyCurrentAdjustToAllPages()
                binding.btnApplyToAll.isChecked = false
            }
            hideAdjustPanel()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Adjust panel
    // ─────────────────────────────────────────────────────────────

    private fun showAdjustPanel() {
        isAdjustPanelVisible = true
        binding.bottomBar.isVisible = false
        binding.filterPanel.isVisible = true
        binding.btnApplyToAll.isVisible = viewModel.pages.value.size > 1
        // Lock UI: disable swipe, page indicator, toolbar actions
        binding.viewPager.isUserInputEnabled = false
        binding.bottomControls.isVisible = false
        binding.btnCropPage.isVisible = false
        binding.btnDeletePage.isVisible = false
        setAdjustMode(currentAdjustMode)
    }

    private fun hideAdjustPanel() {
        isAdjustPanelVisible = false
        binding.filterPanel.isVisible = false
        binding.bottomBar.isVisible = true
        // Unlock UI
        binding.viewPager.isUserInputEnabled = true
        binding.bottomControls.isVisible = true
        binding.btnCropPage.isVisible = true
        binding.btnDeletePage.isVisible = viewModel.pages.value.size > 1
    }

    private fun setAdjustMode(mode: AdjustMode) {
        currentAdjustMode = mode
        // Highlight
        binding.btnContrast.isSelected = mode == AdjustMode.CONTRAST
        binding.btnBrightness.isSelected = mode == AdjustMode.BRIGHTNESS
        binding.btnSaturation.isSelected = mode == AdjustMode.SATURATION
        binding.btnWarmth.isSelected = mode == AdjustMode.WARMTH
        // Sync slider với pvView hiện tại
        withCurrentPvView { pv ->
            binding.sliderAdjust.value = when (mode) {
                AdjustMode.BRIGHTNESS -> pvToSlider(pv.brightness)
                AdjustMode.CONTRAST   -> pvToSlider(pv.contrast)
                AdjustMode.SATURATION -> pvToSlider(pv.saturation)
                AdjustMode.WARMTH     -> warmthToSlider(pv.warmth)
            }.coerceIn(-100f, 100f)
            updateAdjustValueText(binding.sliderAdjust.value.toInt())
        }
    }

    private fun syncAdjustToViewModel() {
        withCurrentPvView { pv ->
            viewModel.setAllAdjust(
                brightness = pv.brightness,
                contrast   = pv.contrast,
                saturation = pv.saturation,
                warmth     = pv.warmth
            )
        }
    }

    private fun restoreAdjust(pv: PerspectiveImageView, state: PageState) {
        pv.setBrightness(state.brightness)
        pv.setContrast(state.contrast)
        pv.setSaturation(state.saturation)
        pv.setWarmth(state.warmth)
    }

    // ─────────────────────────────────────────────────────────────
    // Conversion: slider -100..100  ↔  PerspectiveImageView ranges
    //   brightness/contrast/saturation: 0..2 (neutral = 1)
    //   warmth:                         0.5..2 (neutral = 1)
    // ─────────────────────────────────────────────────────────────

    private fun sliderToPv(v: Float): Float = ((v / 100f) + 1f).coerceIn(0f, 2f)
    private fun pvToSlider(pv: Float): Float = ((pv - 1f) * 100f).coerceIn(-100f, 100f)
    private fun sliderToWarmth(v: Float): Float = (v / 100f + 1f).coerceIn(0.5f, 2f)
    private fun warmthToSlider(w: Float): Float = ((w - 1f) * 100f).coerceIn(-100f, 100f)

    private fun updateAdjustValueText(value: Int) {
        binding.tvAdjustValue.text = if (value > 0) "+$value" else "$value"
    }

    // ─────────────────────────────────────────────────────────────
    // Observe
    // ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pages.collect { pages ->
                        pagerAdapter.submitList(pages)
                        updatePageIndicator()
                        binding.btnDeletePage.isVisible = pages.size > 1
                        syncFilterAdapterForPage(viewModel.currentPageIndex.value)
                    }
                }
                launch {
                    viewModel.currentPageIndex.collect { index ->
                        if (binding.viewPager.currentItem != index)
                            binding.viewPager.setCurrentItem(index, false)
                        updatePageIndicator()
                        syncFilterAdapterForPage(index)
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        pagerAdapter.setLoading(loading, viewModel.currentPageIndex.value)
                    }
                }
                launch { viewModel.events.collectLatest { handleEvent(it) } }
            }
        }
    }

    private fun handleEvent(event: PhotoEditorEvent) {
        when (event) {
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

    private fun syncFilterAdapterForPage(index: Int) {
        val state = viewModel.pages.value.getOrNull(index) ?: return
        filterAdapter.setSourceImage(state.uri)
        filterAdapter.setSelectedFilter(state.filterType)
    }

    private inline fun withCurrentPvView(block: (PerspectiveImageView) -> Unit) {
        val rv = binding.viewPager.getChildAt(0) as? RecyclerView ?: return
        val pv = pagerAdapter.getPerspectiveImageView(rv, viewModel.currentPageIndex.value) ?: return
        block(pv)
    }

    private fun showApplyFilterDialog(onApply: () -> Unit) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        val b = DialogPeApplyAllBinding.inflate(layoutInflater)
        b.tvTitle.setText(R.string.pe_apply_filter_title)
        b.tvMessage.setText(R.string.pe_apply_filter_message)
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


