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
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.Slider
import com.ezt.pdfreader.photoeditor.R
import com.ezt.pdfreader.photoeditor.data.EditMode
import com.ezt.pdfreader.photoeditor.data.PageState
import com.ezt.pdfreader.photoeditor.databinding.DialogPeDiscardBinding
import com.ezt.pdfreader.photoeditor.databinding.FragmentPhotoEditorBinding
import com.ezt.pdfreader.photoeditor.databinding.LayoutPeAdjustPanelBinding
import com.ezt.pdfreader.photoeditor.databinding.LayoutPeCropPanelBinding
import com.ezt.pdfreader.photoeditor.databinding.LayoutPeFilterPanelBinding
import com.ezt.pdfreader.photoeditor.databinding.LayoutPeMainToolbarBinding
import com.ezt.pdfreader.photoeditor.ui.activity.PhotoEditorActivity
import com.ezt.pdfreader.photoeditor.ui.adapter.FilterAdapter
import com.ezt.pdfreader.photoeditor.ui.adapter.PagePagerAdapter
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorEvent
import com.ezt.pdfreader.photoeditor.viewmodel.PhotoEditorViewModel
import com.mct.doc.scanner.DocCropUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PhotoEditorFragment : Fragment() {

    private var _binding: FragmentPhotoEditorBinding? = null
    private val binding get() = _binding!!

    // Include bindings
    private lateinit var mainToolbarBinding: LayoutPeMainToolbarBinding
    private lateinit var filterPanelBinding: LayoutPeFilterPanelBinding
    private lateinit var cropPanelBinding: LayoutPeCropPanelBinding
    private lateinit var adjustPanelBinding: LayoutPeAdjustPanelBinding

    private val viewModel: PhotoEditorViewModel by activityViewModels()

    private lateinit var pagerAdapter: PagePagerAdapter
    private lateinit var filterAdapter: FilterAdapter

    private val backPressCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            cancelEdit()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        initBindings()
        setupViewPager()
        setupPageIndicator()
        setupBottomToolbar()
        setupFilterPanel()
        setupCropPanel()
        setupAdjustPanel()
        setupDeleteButton()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ═══════════════════════════════════════════════════════════════
    // Setup Methods
    // ═══════════════════════════════════════════════════════════════

    private fun initBindings() {
        mainToolbarBinding = LayoutPeMainToolbarBinding.bind(binding.bottomToolbar.root)
        filterPanelBinding = LayoutPeFilterPanelBinding.bind(binding.filterPanel.root)
        cropPanelBinding = LayoutPeCropPanelBinding.bind(binding.cropPanel.root)
        adjustPanelBinding = LayoutPeAdjustPanelBinding.bind(binding.adjustPanel.root)
    }

    private fun setupViewPager() {
        pagerAdapter = PagePagerAdapter()
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 1
        (binding.viewPager.getChildAt(0) as? RecyclerView)?.itemAnimator = null
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentPage(position)
            }
        })
    }

    private fun setupPageIndicator() {
        binding.btnPrevPage.setOnClickListener {
            viewModel.previousPage()
        }

        binding.btnNextPage.setOnClickListener {
            viewModel.nextPage()
        }
    }

    private fun setupBottomToolbar() {
        mainToolbarBinding.btnFilter.setOnClickListener {
            viewModel.setEditMode(EditMode.FILTER)
        }

        mainToolbarBinding.btnCrop.setOnClickListener {
            viewModel.setEditMode(EditMode.CROP)
        }

        mainToolbarBinding.btnAdjust.setOnClickListener {
            viewModel.setEditMode(EditMode.ADJUST)
        }
    }

    private fun setupFilterPanel() {
        filterAdapter = FilterAdapter { filterType ->
            viewModel.applyFilter(filterType)
        }

        filterPanelBinding.rvFilters.apply {
            adapter = filterAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            itemAnimator = null
        }

        filterPanelBinding.btnFilterCancel.setOnClickListener {
            cancelEdit()
        }
        filterPanelBinding.btnFilterSubmit.setOnClickListener {
            val activity = requireActivity() as? PhotoEditorActivity
            val currentFilter = viewModel.getCurrentFilter()
            if (currentFilter.isPremium && activity != null && !activity.isPremium()) {
                activity.openPaywall()
            } else {
                viewModel.confirmCurrentEdit()
            }
        }
    }

    private fun setupCropPanel() {
        cropPanelBinding.btnCropCancel.setOnClickListener {
            cancelEdit()
        }
        cropPanelBinding.btnCropSubmit.setOnClickListener {
            syncCornersFromView()
            viewModel.confirmCurrentEdit()
        }

        cropPanelBinding.btnFullCrop.setOnClickListener {
            viewModel.setFullCrop()
        }

        cropPanelBinding.btnAiCrop.setOnClickListener {
            viewModel.detectCornersWithAI()
        }

        cropPanelBinding.btnRotateLeft.setOnClickListener {
            syncCornersFromView()
            viewModel.rotateLeft()
        }

        cropPanelBinding.btnRotateRight.setOnClickListener {
            syncCornersFromView()
            viewModel.rotateRight()
        }

    }

    private fun setupAdjustPanel() {
        // Contrast slider - update text while dragging, update preview on release
        adjustPanelBinding.sliderContrast.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                adjustPanelBinding.tvContrastValue.text = value.toInt().toString()
                viewModel.setContrast(value)
            }
        }
        // Brightness slider - update text while dragging, update preview on release
        adjustPanelBinding.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                adjustPanelBinding.tvBrightnessValue.text = value.toInt().toString()
                viewModel.setBrightness(value)
            }
        }
        // Sharpen slider - update text while dragging, update preview on release
        adjustPanelBinding.sliderSharpen.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                adjustPanelBinding.tvSharpenValue.text = value.toInt().toString()
                viewModel.setSharpen(value)
            }
        }

        adjustPanelBinding.btnAdjustCancel.setOnClickListener {
            cancelEdit()
        }
        adjustPanelBinding.btnAdjustSubmit.setOnClickListener {
            viewModel.confirmCurrentEdit()
        }

        adjustPanelBinding.btnReset.setOnClickListener {
            // Reset sliders UI
            adjustPanelBinding.sliderContrast.value = 0f
            adjustPanelBinding.sliderBrightness.value = 0f
            adjustPanelBinding.sliderSharpen.value = 0f
            adjustPanelBinding.tvContrastValue.text = "0"
            adjustPanelBinding.tvBrightnessValue.text = "0"
            adjustPanelBinding.tvSharpenValue.text = "0"
            // Reset ViewModel state
            viewModel.resetAdjustments()
        }
    }

    private fun setupDeleteButton() {
        binding.btnDeletePage.setOnClickListener {
            viewModel.requestDeleteCurrentPage()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Observe ViewModel
    // ═══════════════════════════════════════════════════════════════

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Pages
                launch {
                    viewModel.pages.collect { pages ->
                        pagerAdapter.submitList(pages)
                        updatePageIndicator()
                        updateDeleteButtonVisibility(pages.size)
                    }
                }

                // Current page index
                launch {
                    viewModel.currentPageIndex.collect { index ->
                        if (binding.viewPager.currentItem != index) {
                            binding.viewPager.setCurrentItem(index, true)
                        }
                        updatePageIndicator()
                    }
                }

                // Edit mode
                launch {
                    viewModel.editMode.collect { mode ->
                        updateUIForMode(mode)
                        backPressCallback.isEnabled = mode != EditMode.PREVIEW
                    }
                }

                // Loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        pagerAdapter.setLoading(isLoading, viewModel.currentPageIndex.value)
                    }
                }

                // Events
                launch {
                    viewModel.events.collectLatest { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UI Updates
    // ═══════════════════════════════════════════════════════════════

    private fun updateUIForMode(mode: EditMode) {
        // ViewPager only interactive in preview mode
        binding.viewPager.isUserInputEnabled = mode == EditMode.PREVIEW

        // Enable/disable crop mask on ViewPager items
        pagerAdapter.setCropModeEnabled(mode == EditMode.CROP, viewModel.currentPageIndex.value)

        // Bottom panels
        binding.bottomToolbar.root.isVisible = mode == EditMode.PREVIEW
        binding.filterPanel.root.isVisible = mode == EditMode.FILTER
        binding.cropPanel.root.isVisible = mode == EditMode.CROP
        binding.adjustPanel.root.isVisible = mode == EditMode.ADJUST

        // Bottom controls (indicator + delete)
        binding.bottomControls.isVisible = mode == EditMode.PREVIEW

        // Delete button - only show when preview mode AND more than 1 page
        binding.btnDeletePage.isVisible = mode == EditMode.PREVIEW && viewModel.pages.value.size > 1

        // Update filter selection and source image
        if (mode == EditMode.FILTER) {
            filterAdapter.setSelectedFilter(viewModel.getCurrentFilter())
            viewModel.pages.value.getOrNull(viewModel.currentPageIndex.value)?.let { state ->
                filterAdapter.setSourceImage(state.uri)
            }
        }

        // Update adjust sliders
        if (mode == EditMode.ADJUST) {
            viewModel.pages.value.getOrNull(viewModel.currentPageIndex.value)?.let { state ->
                adjustPanelBinding.sliderContrast.value = state.contrast
                adjustPanelBinding.sliderBrightness.value = state.brightness
                adjustPanelBinding.sliderSharpen.value = state.sharpen
                adjustPanelBinding.tvContrastValue.text = state.contrast.toInt().toString()
                adjustPanelBinding.tvBrightnessValue.text = state.brightness.toInt().toString()
                adjustPanelBinding.tvSharpenValue.text = state.sharpen.toInt().toString()
            }
        }
    }

    private fun updatePageIndicator() {
        val total = viewModel.pages.value.size
        val current = viewModel.currentPageIndex.value + 1
        binding.tvPageIndicator.text = "$current/$total"

        // Update button states
        binding.btnPrevPage.alpha = if (viewModel.currentPageIndex.value > 0) 1f else 0.3f
        binding.btnNextPage.alpha = if (viewModel.currentPageIndex.value < total - 1) 1f else 0.3f
    }

    private fun updateDeleteButtonVisibility(pageCount: Int) {
        binding.btnDeletePage.isVisible = pageCount > 1 && viewModel.editMode.value == EditMode.PREVIEW
    }

    private fun syncCornersFromView() {
        val recyclerView = binding.viewPager.getChildAt(0) as? RecyclerView ?: return
        val index = viewModel.currentPageIndex.value
        val perspectiveView = pagerAdapter.getPerspectiveImageView(recyclerView, index) ?: return
        val displayCorners = perspectiveView.cornersArray ?: return

        // Scale corners from display coordinates back to original
        val pageState = viewModel.pages.value.getOrNull(index) ?: return
        val drawable = perspectiveView.drawable
        val displayW = drawable?.intrinsicWidth ?: 0
        val displayH = drawable?.intrinsicHeight ?: 0
        val (origW, origH) = pageState.getEffectiveDimensions()

        val originalCorners = if (displayW > 0 && displayH > 0 && origW > 0 && origH > 0) {
            PageState.scaleCorners(displayCorners, displayW, displayH, origW, origH)
        } else {
            displayCorners
        }
        viewModel.updateCorners(originalCorners)
    }

    private fun cancelEdit() {
        val currentIndex = viewModel.currentPageIndex.value
        val changed = viewModel.cancelCurrentEdit()
        if (changed) {
            // Pre-disable crop mode flag to prevent editMode observer from
            // reloading with stale adapter data (race condition with submitList diff)
            pagerAdapter.setCropModeEnabled(false, -1)
            pagerAdapter.notifyItemChanged(currentIndex)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Event Handling
    // ═══════════════════════════════════════════════════════════════

    private fun handleEvent(event: PhotoEditorEvent) {
        when (event) {
            is PhotoEditorEvent.ConfirmDeletePage -> {
                showDiscardDialog(event.index)
            }

            is PhotoEditorEvent.PageDeleted -> {
                Toast.makeText(requireContext(), R.string.pe_page_deleted, Toast.LENGTH_SHORT).show()
            }

            is PhotoEditorEvent.CannotDeleteLastPage -> {
                Toast.makeText(requireContext(), R.string.pe_cannot_delete_last_page, Toast.LENGTH_SHORT).show()
            }

            is PhotoEditorEvent.RotationChanged -> {
                pagerAdapter.updateRotation(viewModel.currentPageIndex.value)
            }

            is PhotoEditorEvent.CornersChanged -> {
                // Update corners without full rebind
                val index = viewModel.currentPageIndex.value
                val pageState = viewModel.pages.value.getOrNull(index)
                pagerAdapter.updateCorners(pageState?.corners, index)
            }

            is PhotoEditorEvent.AdjustChanged -> {
                // Update ColorFilter on current page (real-time preview)
                val index = viewModel.currentPageIndex.value
                val pageState = viewModel.pages.value.getOrNull(index)
                pagerAdapter.updateAdjustFilter(pageState, index)
            }

            is PhotoEditorEvent.Error -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Dialogs
    // ═══════════════════════════════════════════════════════════════

    private fun showDiscardDialog(pageIndex: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogPeDiscardBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDiscard.setOnClickListener {
            viewModel.deletePage(pageIndex)
            dialog.dismiss()
        }

        dialog.show()
    }
}
