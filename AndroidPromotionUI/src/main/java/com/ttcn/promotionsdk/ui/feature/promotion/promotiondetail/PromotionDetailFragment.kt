package com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.di.promotionViewModelFactory
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.databinding.FragmentDetailPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.PromotionServiceSelection
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.ServiceSelectorBottomSheet
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.ServiceSelectorUiItem
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.adapter.PrmCustomFragmentPagerAdapter
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.theme.applier.TabLayoutThemeApplier
import com.ttcn.promotionsdk.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherBanner
import com.ttcn.promotionsdk.ui.utils.loadPromotionVoucherLogo

class PromotionDetailFragment : PRMBaseFragment<FragmentDetailPromotionBinding>() {

    private val viewModelFactory by lazy { promotionViewModelFactory() }

    private val viewModel: PromotionDetailViewModel by viewModels {
        viewModelFactory
    }

    private var tabMediator: TabLayoutMediator? = null
    private var pagerBoundVoucherId: String? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDetailPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
        binding.tvUse.setOnClickListener {
            viewModel.handleAction(PromotionDetailAction.OpenServiceSelector)
        }
        val voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty()
        if (voucherId.isBlank()) {
            showToast(getString(R.string.prm_no_result))
            return
        }
        showDetailLoading()
        viewModel.handleAction(PromotionDetailAction.LoadDetail(voucherId))
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            val detail = state.detail

            if (state.isLoading && detail == null) {
                showDetailLoading()
                return@collectFlow
            }

            if (detail != null) {
                hideDetailLoading()
                bindDetailContent(state, detail)
                return@collectFlow
            }

            // Không có data (voucherId không tồn tại, hoặc API lỗi).
            //
            // Bản cũ `hideDetailLoading(showContent = false)` ẩn sạch content → user nhận một màn
            // trắng, không tab, không biết đang ở đâu. Giờ VẪN dựng khung đầy đủ với dữ liệu trống —
            // đúng như iOS: hai tab "Thông tin chi tiết" / "Hướng dẫn sử dụng" vẫn có, nội dung để
            // trống, ảnh rơi về placeholder xám mặc định của layout.
            hideDetailLoading()
            bindEmptyContent()
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is PromotionDetailEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
                is PromotionDetailEffect.ShowServiceSelector -> showServiceSelector(effect.services)
            }
        }
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        pagerBoundVoucherId = null
        super.onDestroyView()
    }

    private fun showDetailLoading() {
        binding.shimmerProvider.shimmerDetail.startShimmer()
        binding.shimmerProvider.root.isVisible = true
        binding.contentContainer.isVisible = false
    }

    private fun hideDetailLoading(showContent: Boolean = true) {
        binding.shimmerProvider.shimmerDetail.stopShimmer()
        binding.shimmerProvider.root.isVisible = false
        binding.contentContainer.isVisible = showContent
    }

    private fun bindDetailContent(
        state: PromotionDetailUiState,
        detail: VoucherDetail,
    ) {
        binding.imgBanner.loadPromotionVoucherBanner(
            detail.banner.orEmpty(),
            preferCache = false,
        )
        binding.circleLogo.background = null
        binding.circleLogo.loadPromotionVoucherLogo(
            detail.logo.orEmpty(),
            preferCache = true,
        )
        binding.txtVoucherName.text = detail.merchantName.orEmpty()
        binding.tvContent.text = detail.title.orEmpty()
        binding.tvExpired.text = getString(
            R.string.prm_expiry_short_format,
            detail.expirationDate.orEmpty().toVoucherDisplayDate(),
        )
        binding.tvUse.isVisible = state.actionVisible
        binding.tvUse.isEnabled = state.actionEnabled
        binding.tvUse.text = state.actionLabel.ifBlank {
            if (state.status == VoucherStatus.ACTIVE) getString(R.string.prm_use_now)
            else detail.displayStatusLabel.orEmpty()
        }

        bindDetailTabsIfNeeded(
            voucherId = detail.voucherId,
            descriptionHtml = resolveHtmlContent(detail.description),
            guidelineHtml = resolveHtmlContent(detail.guideline),
        )
    }

    companion object {
        private const val KEY_VOUCHER_ID = "prm_promotion_detail_voucher_id"

        fun newInstance(voucherId: String): PromotionDetailFragment {
            return PromotionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_VOUCHER_ID, voucherId)
                }
            }
        }
    }

    /**
     * Khung màn khi KHÔNG có data — vẫn đủ 2 tab, chữ để trống, ảnh xám mặc định.
     *
     * CỐ Ý không đụng vào `imgBanner` và `circleLogo`: layout đã khai sẵn
     * `@drawable/prm_bg_image_placeholder` và `@drawable/prm_background_shimmer_circle`, nên cứ để
     * nguyên là ra đúng ảnh xám. Gọi `loadPromotionVoucherBanner("")` ở đây chỉ tổ nạp lại đúng cái
     * placeholder đó qua Glide, còn `circleLogo.background = null` (như [bindDetailContent] làm) sẽ
     * **xoá mất** vòng tròn xám.
     *
     * Tab vẫn dựng với nội dung rỗng để user thấy màn hình có cấu trúc, không phải khoảng trắng.
     */
    private fun bindEmptyContent() {
        binding.txtVoucherName.text = ""
        binding.tvContent.text = ""
        binding.tvExpired.text = ""
        binding.tvUse.isVisible = false

        bindDetailTabsIfNeeded(
            voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty(),
            descriptionHtml = "",
            guidelineHtml = "",
        )
    }

    private fun bindDetailTabsIfNeeded(
        voucherId: String,
        descriptionHtml: String,
        guidelineHtml: String,
    ) {
        if (pagerBoundVoucherId == voucherId && binding.viewPager.adapter != null) return
        pagerBoundVoucherId = voucherId
        tabMediator?.detach()

        val adapter = PrmCustomFragmentPagerAdapter(requireActivity())
        adapter.addFragment(
            PrmContentDetailEndowFragment.newInstance(descriptionHtml),
            getString(R.string.prm_tab_detail_info),
        )
        adapter.addFragment(
            PrmContentDetailEndowFragment.newInstance(guidelineHtml),
            getString(R.string.prm_tab_usage_guide),
        )
        binding.viewPager.adapter = adapter
        tabMediator = TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.also { it.attach() }

        binding.tabs.tabMode = TabLayout.MODE_FIXED
        binding.tabs.tabGravity = TabLayout.GRAVITY_FILL
        binding.tabs.isTabIndicatorFullWidth = true
        applyTabUnderlineTheme()
    }

    private fun applyTabUnderlineTheme() {
        val token = PromotionThemeRegistry.tabUnderlineToken()
        TabLayoutThemeApplier.apply(binding.tabs, token)
        token?.backgroundColor?.let { binding.vIndicator.setBackgroundColor(it) }
    }

    private fun resolveHtmlContent(html: String?): String {
        return html?.takeIf { it.isNotBlank() }.orEmpty()
    }

    private fun showServiceSelector(services: List<ServiceSelectorUiItem>) {
        if (childFragmentManager.findFragmentByTag(ServiceSelectorBottomSheet.TAG) != null) return
        ServiceSelectorBottomSheet.newInstance(
            services = services,
            onServiceSelected = { service ->
                // Báo host (đối ứng iOS onServiceSelected) rồi vẫn để VM xử lý điều hướng nội bộ.
                PromotionSDK.getCallback()?.onServiceSelected(
                    PromotionServiceSelection(
                        voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty(),
                        serviceCode = service.serviceCode,
                        serviceName = service.serviceName,
                        iconUrl = service.iconUrl,
                    )
                )
                viewModel.handleAction(PromotionDetailAction.ServiceSelected(service))
            },
        ).show(childFragmentManager, ServiceSelectorBottomSheet.TAG)
    }

    private fun mapErrorMessage(error: String): String {
        return when (error) {
            ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
            "error_detail_unavailable" -> getString(R.string.prm_no_result)
            else -> getString(R.string.prm_error_general)
        }
    }
}
