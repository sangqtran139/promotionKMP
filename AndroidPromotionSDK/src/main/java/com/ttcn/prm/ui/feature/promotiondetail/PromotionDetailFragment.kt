package com.ttcn.prm.ui.feature.promotiondetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.ttcn.promotionsdk.presentation.base.PRMEffect
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailIntent
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailState
import com.ttcn.prm.ui.di.promotionViewModelFactory
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ttcn.prm.R
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDetail
import com.ttcn.prm.databinding.PrmFragmentDetailPromotionBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.PromotionSDKCallback
import com.ttcn.prm.entry.PromotionServiceSelection
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorBottomSheet
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorUiItem
import com.ttcn.prm.ui.feature.promotiondetail.adapter.PrmCustomFragmentPagerAdapter
import com.ttcn.prm.ui.feature.promotiondetail.adapter.PrmPagerTab
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.theme.applier.TabLayoutThemeApplier
import com.ttcn.prm.ui.utils.extension.toVoucherDisplayDate
import com.ttcn.prm.ui.utils.loadPromotionVoucherBanner
import com.ttcn.prm.ui.utils.loadPromotionVoucherLogo

internal class PromotionDetailFragment : PRMBaseFragment<PrmFragmentDetailPromotionBinding>() {

    private val viewModel: PromotionDetailViewModel by viewModels { promotionViewModelFactory() }

    private var tabMediator: TabLayoutMediator? = null
    private var pagerAdapter: PrmCustomFragmentPagerAdapter? = null

    /**
     * Kênh trả voucher về **đúng nơi đã mở màn này**, chỉ chạy khi [returnVoucherOnApply] bật.
     * Dùng cho `PromotionSDK.openPromotionDetail(...)`: host mở từ màn bất kỳ và nhận lại `voucherId`
     * ngay tại lời gọi đó, thay vì qua [PromotionSDKCallback] singleton (kênh đó không biết màn nào gọi).
     *
     * Đối ứng `PromotionDetailBuilder.DataModel.onVoucherApplied` bên iOS.
     *
     * Tách khỏi `setFragmentResult` trong [onActionClick] vì hai người nhận khác nhau: fragment
     * result dành cho `ChoosePromotionFragment` nội bộ SDK (tick lại ô chọn), closure này dành cho
     * host. Một lần mở màn chỉ có một trong hai thực sự có người nghe.
     *
     * Không sống qua process death (là `var` thường, không vào [arguments]) — cùng giới hạn với
     * `ChoosePromotionFragment.onApplySelectedOffers`. Bị mất thì bấm "Áp dụng" vẫn đóng màn, chỉ là
     * host không nhận được data.
     */
    internal var onVoucherApplied: ((detail: VoucherDetail) -> Unit)? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        PrmFragmentDetailPromotionBinding.inflate(inflater, container, false)

    /**
     * Quyết định **nhãn nút và hành vi khi bấm** (TLNV MOB_002 control #5):
     * - `false` (mặc định): "Sử dụng ngay" → chọn dịch vụ (1 dịch vụ thì đi thẳng).
     * - `true`: "Áp dụng" → trả `voucherId` về nơi đã mở màn rồi đóng màn này.
     *
     * Đối ứng `PromotionDetailBuilder.DataModel.returnVoucherOnApply` bên iOS.
     */
    private val returnVoucherOnApply: Boolean
        get() = arguments?.getBoolean(KEY_RETURN_VOUCHER_ON_APPLY) ?: false

    /**
     * `true` → bấm "Áp dụng" xong SDK **không** tự đóng màn; host tự pop trong [onVoucherApplied].
     * Dùng khi host muốn giữ màn lại thêm (hỏi xác nhận, chạy animation riêng, điều hướng chỗ khác).
     *
     * Chỉ có nghĩa khi [returnVoucherOnApply] bật — nhánh "Sử dụng ngay" không đóng màn bao giờ.
     * Đối ứng `PromotionDetailBuilder.DataModel.hostHandlesDismiss` bên iOS.
     */
    private val hostHandlesDismiss: Boolean
        get() = arguments?.getBoolean(KEY_HOST_HANDLES_DISMISS) ?: false

    override fun setupUI() {
        // `tvUse` là `invisible` (không `gone`) ngay từ XML — luôn được layout đúng chỗ (đóng góp vào
        // constraint `ctlContent`) kể cả lúc chưa có data, không phải đợi data load hay bắt transition
        // visibility nào về sau mới gọi được `applyNavigationBarInset`.
        applyNavigationBarInset(binding.tvUse)
        // `shimmer_button` (placeholder nút "Sử dụng ngay" trong lúc tải) neo đáy CỐ ĐỊNH, không tự
        // né nav bar như `tvUse` thật — thiếu dòng này, trên host không tự chừa chỗ (edge-to-edge
        // trần) placeholder sẽ bị nav bar đè lúc đang shimmer, rồi "nhảy" vị trí đột ngột khi data về
        // và `tvUse` thật hiện ra ở chỗ đúng. Cùng cơ chế, cùng anchor logic — chỉ khác view đích.
        applyNavigationBarInset(binding.shimmerProvider.shimmerButton)
        binding.imgBack.setOnClickListener { goBack() }
        binding.tvUse.setOnClickListener { onActionClick() }
        setupDetailTabs()
        val voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty()
        if (voucherId.isBlank()) {
            // Không mở được chi tiết (thiếu voucherId) → thoát im lặng, SDK không dùng toast nữa.
            return
        }
        showDetailLoading()
        viewModel.dispatch(PromotionDetailIntent.LoadDetail(voucherId))
    }

    override fun observeData() {
        collectFlow(viewModel.state) { state ->
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

            // Không có data (voucherId không tồn tại, hoặc API lỗi): vẫn dựng khung đầy đủ với dữ
            // liệu trống — hai tab "Thông tin chi tiết" / "Hướng dẫn sử dụng" vẫn có, nội dung để
            // trống, ảnh rơi về placeholder xám mặc định của layout.
            hideDetailLoading()
            bindEmptyContent()
        }
        collectFlow(viewModel.effects) { effect ->
            when (effect) {
                // KHÔNG hiện gì: SDK đã bỏ toast. Vẫn thu effect để store `ConsumeError` chạy đúng
                // vòng của nó — bỏ luôn `collectFlow` thì `errorCode` nằm lại trong state.
                // Màn này còn empty-view/list cũ nên user vẫn hiểu được chuyện gì xảy ra.
                is PRMEffect.ShowError -> Unit
            }
        }
    }

    override fun onDestroyView() {
        tabMediator?.detach()
        tabMediator = null
        // Bỏ adapter khỏi pager trước khi view chết: adapter còn bám view đã huỷ là một đường rò.
        binding.viewPager.adapter = null
        pagerAdapter = null
        super.onDestroyView()
    }

    private fun showDetailLoading() {
        binding.shimmerProvider.shimmerDetail.startShimmer()
        binding.shimmerProvider.root.isVisible = true
        // `View.INVISIBLE`, KHÔNG `contentContainer.isVisible = false` (extension đó set `GONE`) —
        // `tvUse` phải luôn được layout kể cả lúc đang tải, xem comment trong
        // `prm_fragment_detail_promotion.xml`.
        binding.contentContainer.visibility = View.INVISIBLE
    }

    private fun hideDetailLoading(showContent: Boolean = true) {
        binding.shimmerProvider.shimmerDetail.stopShimmer()
        binding.shimmerProvider.root.isVisible = false
        binding.contentContainer.visibility = if (showContent) View.VISIBLE else View.INVISIBLE
    }

    private fun bindDetailContent(
        state: PromotionDetailState,
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
        binding.tvExpired.setTextColor(ContextCompat.getColor(requireContext(), R.color.prm_tokenDark60))
        when {
            displayDate.isNotBlank() -> {
                binding.tvExpired.isVisible = true
                binding.tvExpired.text = prmString(R.string.prm_expiry_short_format, displayDate)
            }
            rawExpiration.isNullOrBlank() -> {
                binding.tvExpired.isVisible = true
                binding.tvExpired.text = prmString(R.string.prm_expiry_never)
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
        // iOS `applyTitle`. Nhãn phụ thuộc [returnVoucherOnApply]:
        // bật → "Sử dụng ngay"; tắt → "Áp dụng".
        //
        // LƯU Ý: chỉ đổi CHỮ, không đổi hành vi bấm ở [onActionClick] — bật vẫn trả voucherId/đóng
        // màn (không mở bottomsheet chọn dịch vụ). Cố ý theo yêu cầu, KHÁC với bảng nhãn/hành vi ở
        // TLNV MOB_002 control #5 (xem docs/features/PromotionDetail.md) — nhãn không còn khớp hành
        // vi thực tế khi bấm trong nhánh `returnVoucherOnApply = true`.
        binding.tvUse.text = getString(
            if (returnVoucherOnApply) R.string.prm_apply else R.string.prm_use_now
        )

        bindDetailTabs(
            descriptionHtml = resolveHtmlContent(detail.description),
            guidelineHtml = resolveHtmlContent(detail.guideline),
        )
    }

    companion object {
        private const val KEY_VOUCHER_ID = "prm_promotion_detail_voucher_id"
        private const val KEY_RETURN_VOUCHER_ON_APPLY = "prm_promotion_detail_return_voucher"
        private const val KEY_HOST_HANDLES_DISMISS = "prm_promotion_detail_host_dismiss"

        /** Key `setFragmentResult` khi bấm "Áp dụng" — màn "Chọn ưu đãi" lắng nghe để tick voucher. */
        const val RESULT_APPLY_VOUCHER = "prm_promotion_detail_apply_voucher"

        /** Bundle key chứa voucherId trong [RESULT_APPLY_VOUCHER]. */
        const val RESULT_KEY_VOUCHER_ID = KEY_VOUCHER_ID

        /**
         * Navigation chỉ mang `voucherId` (+ cờ [returnVoucherOnApply]) — màn tự fetch chi tiết,
         * **không** nhận dữ liệu dựng sẵn từ màn danh sách. Trong lúc chờ hiện shimmer. Giống iOS.
         */
        internal fun newInstance(
            voucherId: String,
            returnVoucherOnApply: Boolean = false,
            hostHandlesDismiss: Boolean = false,
        ): PromotionDetailFragment {
            return PromotionDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_VOUCHER_ID, voucherId)
                    putBoolean(KEY_RETURN_VOUCHER_ON_APPLY, returnVoucherOnApply)
                    putBoolean(KEY_HOST_HANDLES_DISMISS, hostHandlesDismiss)
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

        bindDetailTabs(descriptionHtml = "", guidelineHtml = "")
    }

    /**
     * Dựng pager **một lần cho mỗi view** rồi giữ nguyên; nội dung tab về sau đi qua [bindDetailTabs].
     *
     * Không gán adapter mới mỗi lần có data: adapter mới không biết state mà adapter cũ đã lưu trong
     * cùng `childFragmentManager`, và `TabLayoutMediator` thì phải detach/attach lại theo.
     */
    private fun setupDetailTabs() {
        // `this`, không phải `requireActivity()`: tab thuộc childFragmentManager của màn này — xem
        // KDoc của adapter.
        val adapter = PrmCustomFragmentPagerAdapter(this)
        pagerAdapter = adapter
        binding.viewPager.adapter = adapter
        tabMediator = TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.also { it.attach() }

        binding.tabs.tabMode = TabLayout.MODE_FIXED
        binding.tabs.tabGravity = TabLayout.GRAVITY_FILL
        binding.tabs.isTabIndicatorFullWidth = true
        applyTabUnderlineTheme()
    }

    /**
     * Đổ nội dung 2 tab. Gọi lại bao nhiêu lần cũng được: adapter so [PrmPagerTab.contentKey] nên
     * cùng nội dung thì không dựng lại tab, khác nội dung thì tab tự dựng lại.
     */
    private fun bindDetailTabs(descriptionHtml: String, guidelineHtml: String) {
        pagerAdapter?.setTabs(
            listOf(
                PrmPagerTab(
                    title = prmString(R.string.prm_tab_detail_info),
                    contentKey = descriptionHtml,
                    createFragment = { PrmContentDetailOfferFragment.newInstance(descriptionHtml) },
                ),
                PrmPagerTab(
                    title = prmString(R.string.prm_tab_usage_guide),
                    contentKey = guidelineHtml,
                    createFragment = { PrmContentDetailOfferFragment.newInstance(guidelineHtml) },
                ),
            )
        )
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
     * Bấm nút hành động — hai hành vi tuỳ [returnVoucherOnApply] (TLNV MOB_002 control #5):
     * - Tắt (từ "Ưu đãi của tôi" / Tìm kiếm): chọn dịch vụ để dùng ngay.
     * - Bật (từ "Chọn ưu đãi", hoặc host mở thẳng): **không** chọn dịch vụ; trả voucherId về nơi đã
     *   mở màn rồi đóng màn này.
     *
     * Hai người nhận, bắn cả hai vì mỗi lần mở chỉ một bên thực sự có người nghe:
     * [RESULT_APPLY_VOUCHER] cho `ChoosePromotionFragment` nội bộ, [onVoucherApplied] cho host.
     *
     * Gọi **trước** [goBack] để nơi nhận có data ngay khi màn của họ hiện lại — đối ứng iOS
     * (`notifyVoucherApplied()` rồi mới `routeToParent()`). Thứ tự này cũng là thứ khiến
     * [hostHandlesDismiss] chạy được: host nhận data lúc màn vẫn còn sống, rồi tự quyết khi nào đóng.
     */
    private fun onActionClick() {
        if (returnVoucherOnApply) {
            val voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty()
            requireActivity().supportFragmentManager.setFragmentResult(
                RESULT_APPLY_VOUCHER,
                bundleOf(RESULT_KEY_VOUCHER_ID to voucherId),
            )
            // Detail chỉ có sau khi API trả; nút "Áp dụng" bị khoá trước đó nên bình thường không
            // null. Null thì bỏ callback — không bịa object rỗng cho host.
            viewModel.state.value.detail?.let { onVoucherApplied?.invoke(it) }
            if (!hostHandlesDismiss) goBack()
            return
        }
        showServiceSelector(viewModel.serviceOptions())
    }

    /**
     * Một dịch vụ đã được chọn — dù qua bottom sheet hay đi thẳng (chỉ có 1 dịch vụ khả dụng,
     * chỉ có 1 dịch vụ khả dụng). Báo host — đối ứng iOS `onServiceSelected`.
     */
    private fun onServiceSelected(service: ServiceSelectorUiItem) {
        PromotionSDK.getCallback()?.onServiceSelected(
            PromotionServiceSelection(
                voucherId = arguments?.getString(KEY_VOUCHER_ID).orEmpty(),
                productId = service.productId,
                productName = service.productName,
                skuSourceId = service.skuSourceId,
                iconUrl = service.iconUrl,
            )
        )
    }

}
