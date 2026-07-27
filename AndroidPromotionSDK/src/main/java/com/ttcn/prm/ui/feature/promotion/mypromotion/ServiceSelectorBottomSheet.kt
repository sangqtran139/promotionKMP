package com.ttcn.prm.ui.feature.promotion.mypromotion

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ttcn.prm.R
import com.ttcn.prm.databinding.BottomSheetServiceSelectorBinding
import com.ttcn.prm.ui.feature.promotion.mypromotion.adapter.ServiceSelectorAdapter

internal class ServiceSelectorBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetServiceSelectorBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var services: List<ServiceSelectorUiItem> = emptyList()
    private var onServiceSelected: ((ServiceSelectorUiItem) -> Unit)? = null

    private val adapter = ServiceSelectorAdapter { service ->
        onServiceSelected?.invoke(service)
        dismiss()
    }

    override fun getTheme(): Int = R.style.PRMBaseBottomSheetDialog

    /** Ép LIGHT (force-dark=false) như [com.ttcn.prm.ui.base.PRMBaseFragment] — bottom sheet không qua base. */
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val base = super.onGetLayoutInflater(savedInstanceState)
        val themed = ContextThemeWrapper(requireContext(), R.style.PRMForceLight)
        return base.cloneInContext(themed)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetServiceSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvServices.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
            adapter = this@ServiceSelectorBottomSheet.adapter
            itemAnimator = null
        }
        adapter.submitList(services)
        val hasServices = services.isNotEmpty()
        binding.rvServices.isVisible = hasServices
        binding.tvEmpty.isVisible = !hasServices
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onServiceSelected = null
    }

    companion object {
        private const val GRID_SPAN_COUNT = 3
        const val TAG = "prm_service_selector"

        fun newInstance(
            services: List<ServiceSelectorUiItem>,
            onServiceSelected: (ServiceSelectorUiItem) -> Unit,
        ): ServiceSelectorBottomSheet = ServiceSelectorBottomSheet().apply {
            this.services = services
            this.onServiceSelected = onServiceSelected
        }
    }
}
