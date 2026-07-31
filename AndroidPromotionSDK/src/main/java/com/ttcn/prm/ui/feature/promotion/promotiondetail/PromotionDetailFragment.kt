package com.ttcn.prm.ui.feature.promotion.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ttcn.prm.R
import com.ttcn.prm.ui.di.promotionViewModelFactory
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherDetail
import com.ttcn.prm.databinding.FragmentDetailPromotionBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.PromotionServiceSelection
import com.ttcn.prm.ui.di.PromotionViewModelFactory
import com.ttcn.prm.ui.feature.promotion.mypromotion.ServiceSelectorBottomSheet
import com.ttcn.prm.ui.feature.promotion.mypromotion.ServiceSelectorUiItem
import com.ttcn.prm.ui.feature.promotion.promotiondetail.adapter.PrmCustomFragmentPagerAdapter
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.theme.applier.TabLayoutThemeApplier
import com.ttcn.prm.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.prm.ui.utils.loadPromotionVoucherBanner
import com.ttcn.prm.ui.utils.loadPromotionVoucherLogo

class PromotionDetailFragment : PRMBaseFragment<FragmentDetailPromotionBinding>() {

    private val viewModelFactory by lazy { promotionViewModelFactory() }

    private val viewModel: PromotionDetailViewModel by viewModels {
        viewModelFactory
    }

    private var tabMediator: TabLayoutMediator? = null
    private var pagerBoundVoucherId: String? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDetailPromotionBinding.inflate(inflater, container, false)

    /** Nơi mở màn này — quyết định nhãn nút + hành vi khi bấm. Xem [PromotionDetailEntry]. */
    private val entry: PromotionDetailEntry
        get() = arguments?.getString(KEY_ENTRY)
            ?.let { runCatching { PromotionDetailEntry.valueOf(it) }.getOrNull() }
            ?: PromotionDetailEntry.MY_PROMOTION

    override fun setupUI() {
        binding.imgBack.setOnClickListener { onBackFragment() }
        binding.tvUse.setOnClickListener { onActionClick() }
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
                is PromotionDetailEffect.ShowError -> showToast(mapPromotionError(effect.errorCode))
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
        // Dòng HSD — luôn hiện ngày thô, không tô cam / không hiện "còn X ngày" (khác màn danh sách).
        val rawExpiration = detail.expirationDate
        val displayDate = rawExpiration.orEmpty().toVoucherDisplayDate()
        binding.tvExpired.setTextColor(ContextCompat.getColor(requireContext(), R.color.tokenDark60))
        when {
            displayDate.isNotBlank() -> {
                binding.tvExpired.isVisible = true
                binding.tvExpired.text = getString(R.string.prm_expiry_short_format, displayDate)
            }
            rawExpiration.isNullOrBlank() -> {
                binding.tvExpired.isVisible = true
                binding.tvExpired.text = getString(R.string.prm_expiry_never)
            }
            else -> binding.tvExpired.isVisible = false
        }
        binding.tvUse.visibility = if (state.actionVisible) View.VISIBLE else View.INVISIBLE
        binding.tvUse.isEnabled = state.actionEnabled
        // Nút "Sử dụng ngay" hiện cho MỌI trạng thái usable (actionEnabled do store quyết định) —
        // khớp iOS/store, không khoá riêng ACTIVE (AVAILABLE/USABLE/AVAILABLE_TO_CLAIM cũng usable).
        //
        // NHÃN: **cố định chuỗi SDK**, KHÔNG dùng `state.actionLabel` (= `displayStatusLabel` của
        // server). Server đang trả "Sử dụng" cho mọi voucher, còn màn này muốn chuỗi riêng — đối ứng
        // iOS `applyTitle`. Nhãn phụ thuộc nơi mở màn (TLNV MOB_002 control #5):
        // từ "Ưu đãi của tôi" → "Sử dụng ngay"; từ luồng thanh toán → "Áp dụng".
        binding.tvUse.text = getString(
            if (entry == PromotionDetailEntry.CHECKOUT) R.string.prm_apply else R.string.prm_use_now
        )

        bindDetailTabsIfNeeded(
            voucherId = detail.voucherId,
            descriptionHtml = resolveHtmlContent(detail.description),
            guidelineHtml = resolveHtmlContent(detail.guideline),
        )
    }

    companion object {
        private const val KEY_VOUCHER_ID = "prm_promotion_detail_voucher_id"
        private const val KEY_ENTRY = "prm_promotion_detail_entry"

        /** Key `setFragmentResult` khi bấm "Áp dụng" — màn "Chọn ưu đãi" lắng nghe để tick voucher. */
        const val RESULT_APPLY_VOUCHER = "prm_promotion_detail_apply_voucher"

        /** Bundle key chứa voucherId trong [RESULT_APPLY_VOUCHER]. */
        const val RESULT_KEY_VOUCHER_ID = KEY_VOUCHER_ID

        /**
         * Navigation chỉ mang `voucherId` (+ nơi mở màn) — màn tự fetch chi tiết, **không** nhận dữ
         * liệu dựng sẵn từ màn danh sách. Trong lúc chờ hiện shimmer. Giống iOS.
         */
        internal fun newInstance(
            voucherId: String,
            entry: PromotionDetailEntry = PromotionDetailEntry.MY_PROMOTION,
        ): PromotionDetailFragment {
            return PromotionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_VOUCHER_ID, voucherId)
                    putString(KEY_ENTRY, entry.name)
                }
            }
        }
    }

    /**
     * Khung màn khi KHÔNG có data — vẫn đủ 2 tab, chữ để trống, ảnh xám mặc định.
     *
     * CỐ Ý không đụng vào `imgBanner` và `circleLogo`: layout đã khai sẵn
     * `@drawable/prm_bg_image_placeholder` và `@drawable/prm_bg_image_placeholder_circle`, nên cứ để
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
        binding.tvUse.visibility = View.INVISIBLE

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
        // `present` lo luôn: 1 dịch vụ → chọn thẳng không mở sheet, và toast xác nhận. Xem KDoc ở đó.
        ServiceSelectorBottomSheet.present(this, services, ::onServiceSelected)
    }

    /**
     * Bấm nút hành động — hai hành vi tuỳ nơi mở màn (TLNV MOB_002 control #5):
     * - Từ "Ưu đãi của tôi": chọn dịch vụ để dùng ngay.
     * - Từ luồng thanh toán: **không** chọn dịch vụ; trả voucherId về màn "Chọn ưu đãi" (tick sẵn
     *   ô chọn) rồi đóng màn này.
     */
    private fun onActionClick() {
        if (entry == PromotionDetailEntry.CHECKOUT) {
            requireActivity().supportFragmentManager.setFragmentResult(
                RESULT_APPLY_VOUCHER,
                bundleOf(RESULT_KEY_VOUCHER_ID to arguments?.getString(KEY_VOUCHER_ID).orEmpty()),
            )
            onBackFragment()
            return
        }
        viewModel.handleAction(PromotionDetailAction.OpenServiceSelector)
    }

    /**
     * Một dịch vụ đã được chọn — dù qua bottom sheet hay đi thẳng (chỉ có 1 dịch vụ khả dụng,
     * effect [PromotionDetailEffect.ServiceChosen]). Báo host (đối ứng iOS `onServiceSelected`)
     * rồi vẫn để VM xử lý điều hướng nội bộ.
     */
    private fun onServiceSelected(service: ServiceSelectorUiItem) {
        PromotionSDK.getCallback()?.onServiceSelected(
            PromotionServiceSelection(
                voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty(),
                serviceCode = service.serviceCode,
                serviceName = service.serviceName,
                iconUrl = service.iconUrl,
            )
        )
        viewModel.handleAction(PromotionDetailAction.ServiceSelected(service))
    }

}
