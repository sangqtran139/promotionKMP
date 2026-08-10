package com.ttcn.prm.ui.feature.choosepromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.ttcn.prm.ui.di.promotionViewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.presentation.common.PROMOTION_SEARCH_MAX_LENGTH
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseSeeMoreState
import com.ttcn.prm.databinding.PrmFragmentChoosePromotionBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.ui.base.PromotionToastGate
import com.ttcn.prm.ui.feature.choosepromotion.adapter.ChoosePromotionListItem
import com.ttcn.prm.ui.feature.choosepromotion.adapter.ChoosePromotionMainAdapter
import com.ttcn.prm.ui.feature.ext.toVoucherListItem
import com.ttcn.promotionsdk.presentation.base.PRMEffect
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.mySeeMoreState
import com.ttcn.promotionsdk.presentation.choosepromotion.visibleMyOffers
import com.ttcn.prm.ui.feature.endowview.PRMEndowView
import com.ttcn.prm.ui.feature.promotiondetail.PromotionDetailFragment
import com.ttcn.prm.ui.utils.extension.VerticalSpaceItemDecoration

internal class ChoosePromotionFragment : PRMBaseFragment<PrmFragmentChoosePromotionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        PrmFragmentChoosePromotionBinding.inflate(inflater, container, false)

    // ─── Input ────────────────────────────────────────────────────────────────

    /**
     * Data đã load sẵn từ [PRMEndowView] — dùng lại để tránh double API call.
     * Rỗng thì ViewModel tự gọi API.
     *
     * `internal`: [EligibleOffer] thuộc `promotionLogic`. Host dựng màn qua [forEndowView].
     */
    internal var initialMyOffers: List<EligibleOffer> = emptyList()
    internal var initialOtherOffers: List<EligibleOffer> = emptyList()
    /** Cờ phân trang đi kèm dữ liệu preload — quyết định nút "Xem thêm" và có gọi trang kế không. */
    internal var initialMyIsLastPage: Boolean = true
    internal var initialOtherIsLastPage: Boolean = true

    /** objectId của các voucher cần pre-select (valid=true từ discountDetails trước đó). */
    internal var preSelectedVoucherIds: Set<String> = emptySet()

    /**
     * Callback trả về **offers đang chọn** khi bấm "Áp dụng"; validate + áp do `EndowStore` lo.
     * [forEndowView] tự nối vào [PRMEndowView.applySelectedOffers].
     *
     * Tham số thứ hai là hàm báo **đã validate xong** kèm mã lỗi (`null` = thành công) — màn chỉ
     * đóng khi áp được, lỗi thì ở lại + báo. Đối ứng completion của `endowVM.validateAndApply` iOS.
     *
     * `internal`: [EligibleOffer] thuộc `promotionLogic`.
     */
    internal var onApplySelectedOffers: ((List<EligibleOffer>, (String?) -> Unit) -> Unit)? = null

    // ─── Internal state ───────────────────────────────────────────────────────
    // Selection + trạng thái mở/thu gọn nay do store (promotionLogic) quản — Fragment chỉ render.

    private lateinit var mainAdapter: ChoosePromotionMainAdapter

    private val viewModel: ChoosePromotionViewModel by viewModels { promotionViewModelFactory() }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun setupUI() {
        setupRecyclerView()
        setupButtons()
        setupSearch()
        listenApplyFromDetail()
    }

    /**
     * Màn Chi tiết mở từ đây bấm "Áp dụng" → quay lại đây với voucher đó **đã tick** (TLNV MOB_002
     * control #5). Dùng `SetPreSelected` chứ không phải `ToggleSelection`: kết quả phải là "đúng
     * voucher này được chọn", không phụ thuộc trạng thái tick trước đó.
     */
    private fun listenApplyFromDetail() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            PromotionDetailFragment.RESULT_APPLY_VOUCHER,
            viewLifecycleOwner,
        ) { _, bundle ->
            val voucherId = bundle.getString(PromotionDetailFragment.RESULT_KEY_VOUCHER_ID).orEmpty()
            if (voucherId.isBlank()) return@setFragmentResultListener
            viewModel.dispatch(ChoosePromotionIntent.SetPreSelected(listOf(voucherId)))
        }
    }

    override fun observeData() {
        super.observeData()

        collectFlow(viewModel.state) { state ->
            // Phải ẩn list khi shimmer hiện: trong `fragment_choose_promotion.xml`, `shimmer_provider`
            // là con ĐẦU TIÊN còn `rcvVoucher` là con sau nó, mà RecyclerView có nền đục
            // (`@color/prm_color_f4f4f4`) nên vẽ đè kín shimmer. Đối ứng `homeList.isVisible` ở
            // `MyPromotionFragment`.
            binding.shimmerProvider.root.isVisible = state.isLoading

            // Gõ từ khoá mà không ra gì → view "không tìm thấy" thay cho list, giống màn "Tìm ưu đãi"
            // (`SearchMyPromotionFragment.renderState`). Danh sách rỗng lúc KHÔNG tìm kiếm thì để
            // nguyên list trống — đó là "chưa có ưu đãi nào", không phải "tìm không ra".
            val showNoResult = state.keyword.isNotBlank() && !state.isLoading && state.isEmpty
            binding.ctlNoResult.isVisible = showNoResult
            binding.rcvVoucher.isVisible = !state.isLoading && !showNoResult

            updateApplyButtonState(state)
            rebuildList(state)
        }

        collectFlow(viewModel.effects) { effect ->
            when (effect) {
                is PRMEffect.ShowError -> showToast(mapPromotionError(effect.errorCode))
            }
        }

        // Seed voucher pre-select vào store trước khi load (store giữ selection).
        viewModel.dispatch(ChoosePromotionIntent.SetPreSelected(preSelectedVoucherIds.toList()))

        // Truyền data đã load sẵn; nếu rỗng → ViewModel tự gọi API
        viewModel.dispatch(
            ChoosePromotionIntent.Preload(
                myOffers = initialMyOffers,
                otherOffers = initialOtherOffers,
                myIsLastPage = initialMyIsLastPage,
                otherIsLastPage = initialOtherIsLastPage,
            )
        )
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        mainAdapter = ChoosePromotionMainAdapter(
            onVoucherClick = { viewModel.dispatch(ChoosePromotionIntent.ToggleSelection(it.voucherId)) },
            onDetailClick  = { openPromotionDetail(it.voucherId, returnVoucherOnApply = true) },
            // Store chạy state-machine "mở hết → tải trang kế → thu gọn" (gộp cả expand & collapse).
            onSeeMoreMyVoucher = { viewModel.dispatch(ChoosePromotionIntent.SeeMoreMy) },
            onCollapseMyVoucher = { viewModel.dispatch(ChoosePromotionIntent.SeeMoreMy) },
        )

        binding.rcvVoucher.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mainAdapter
            addItemDecoration(
                VerticalSpaceItemDecoration(
                    0,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp),
                )
            )
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    if (lm.findLastVisibleItemPosition() >= lm.itemCount - 2) {
                        viewModel.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
                    }
                }
            })
        }
    }

    private fun rebuildList(state: ChoosePromotionState) {
        val items = mutableListOf<ChoosePromotionListItem>()
        // Từ khoá đang tìm → tô đỏ đoạn khớp trên item (đối ứng `highlightKeyword` bên iOS).
        val highlightKeyword = state.keyword.trim()

        if (state.myOffers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_my_endow)))
            state.visibleMyOffers().forEach { offer ->
                val voucher = offer.toVoucherListItem()
                val isSelected = voucher.voucherId in state.selectedIds
                items.add(
                    ChoosePromotionListItem.VoucherItem(
                        data = voucher.copy(isSelected = isSelected),
                        highlightKeyword = highlightKeyword,
                    )
                )
            }
            // Nút "Xem thêm/Thu gọn": trạng thái tính bằng rule dùng chung ở store (state.mySeeMore).
            val seeMore = state.mySeeMoreState()
            if (seeMore != ChooseSeeMoreState.HIDDEN) {
                items.add(
                    ChoosePromotionListItem.SeeMoreMyVoucher(
                        isExpanded = seeMore == ChooseSeeMoreState.COLLAPSE
                    )
                )
            }
        }

        if (state.otherOffers.isNotEmpty()) {
            // Vạch ngăn chỉ chèn khi có nhóm ở TRÊN nó; danh sách chỉ có "Ưu đãi khác" thì không kẻ.
            // Đứng sau hàng "Xem thêm" (nếu có) nên khoảng cách 8dp giữ nguyên ở cả hai trường hợp.
            if (items.isNotEmpty()) {
                items.add(ChoosePromotionListItem.SectionDivider)
            }
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_endow_different)))
            state.otherOffers.forEach { offer ->
                val voucher = offer.toVoucherListItem()
                val isSelected = voucher.voucherId in state.selectedIds
                items.add(
                    ChoosePromotionListItem.VoucherItem(
                        data = voucher.copy(isSelected = isSelected),
                        highlightKeyword = highlightKeyword,
                    )
                )
            }
            // Đang lấy trang kế của "Ưu đãi khác" → hàng "Đang tải" ở đáy, giống màn "Ưu đãi của tôi".
            // Nhóm "Ưu đãi của tôi" ở màn này phân trang bằng nút "Xem thêm" nên không có hàng này.
            if (state.isLoadingMoreOther) {
                items.add(ChoosePromotionListItem.Loading)
            }
        }

        mainAdapter.submitList(items)
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { goBack() }
        binding.btnApply.setOnClickListener { onApplyClicked() }
    }

    /**
     * Bấm "Áp dụng" → trả offers đang chọn cho widget (`EndowStore` validate).
     * Lỗi → KHÔNG áp; ở lại màn chọn + báo lỗi. Thành công → đóng màn. (Giống iOS.)
     */
    private fun onApplyClicked() {
        val offers = viewModel.selectedOffers()
        if (offers.isEmpty()) return
        val apply = onApplySelectedOffers
        if (apply == null) {
            // Không có widget để áp (fragment bị FragmentManager tái tạo nên mất closure, hoặc host
            // tự dựng màn này không qua `forEndowView`). Báo lỗi rồi Ở LẠI — đóng màn ở đây là nói
            // dối user rằng đã áp xong.
            PromotionToastGate.showAlways(requireContext(), mapPromotionError(ErrorCodes.GENERAL))
            return
        }
        apply(offers) { errorCode ->
            if (errorCode != null) {
                // `showToast` đi qua `PromotionToastGate.isEnabled` — mặc định TẮT, nên lỗi validate
                // bị nuốt hoàn toàn: user bấm "Áp dụng", API hỏng, màn đứng im không một thông báo.
                // Dùng `showAlways` như PRM_MOB_021: user vừa chủ động bấm và đang chờ kết quả, im
                // lặng là không chấp nhận được. (iOS dùng popup `PRMConfirmationDialog`.)
                PromotionToastGate.showAlways(requireContext(), mapPromotionError(errorCode))
            } else {
                goBack()
            }
        }
    }

    /**
     * Thanh "giảm giá" (chỉ hiện ở chế độ multi-select) + trạng thái nút "Áp dụng" — selection lấy
     * từ state store.
     *
     * Chưa chọn voucher nào → **disable** nút: `applySelected()` lọc ra danh sách rỗng và cả hai nền
     * tảng đều bỏ qua, nên để nút bấm được chỉ tạo cảm giác app treo. `PRMButton.setEnabled` tự đổi
     * sang nền `prm_bg_button_primary_disabled`.
     */
    private fun updateApplyButtonState(state: ChoosePromotionState) {
        val show = state.isMultiSelection && state.selectedIds.isNotEmpty()
        binding.layoutReducePrice.isVisible = show
        if (show) {
            binding.txtNumberChooseEndow.text =
                getString(R.string.prm_selected_voucher_count, state.selectedIds.size)
        }
        binding.btnApply.isEnabled = state.selectedIds.isNotEmpty()
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /** Cùng khuôn với `SearchMyPromotionFragment`: gõ mỗi ký tự → `QueryChanged` (ViewModel tự debounce). */
    private fun setupSearch() {
        binding.edtVoucher.apply {
            // Chặn nhập quá giới hạn (TLNV MOB_004 control 2.1 — maxlength 255).
            maxLength = PROMOTION_SEARCH_MAX_LENGTH
            onTextChangeListener = { keyword ->
                if (keyword.isEmpty()) {
                    viewModel.dispatch(ChoosePromotionIntent.ClearKeyword)
                } else {
                    viewModel.dispatch(ChoosePromotionIntent.QueryChanged(keyword))
                }
            }
            setOnSearchActionListener { viewModel.dispatch(ChoosePromotionIntent.Search) }
            setOnDoneKeyboardListener { viewModel.dispatch(ChoosePromotionIntent.Search) }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────


    companion object {
        /**
         * Dựng màn "Chọn ưu đãi" nối sẵn với widget [endowView] ở màn thanh toán.
         *
         * Lấy lại ưu đãi widget đã tải (khỏi gọi `findEligible` lần hai), pre-select voucher đang
         * áp, và đẩy kết quả ngược về widget khi user bấm "Áp dụng".
         *
         * `internal`: fragment này là UI nội bộ. Host gọi qua bề mặt entry, nhận về [Fragment] trần:
         *
         * ```kotlin
         * binding.endowView.onOpenVoucherSelection = {
         *     addFragment(PromotionSDK.createChoosePromotionFragment(binding.endowView))
         * }
         * ```
         *
         * Host muốn làm thêm việc gì đó lúc áp thì ghi đè [onApplySelectedOffers] — nhớ tự gọi
         * `endowView.setDiscountDetails(...)` và hàm báo-đã-xong, vì set lại sẽ thay callback mặc định.
         */
        @JvmStatic
        fun forEndowView(endowView: PRMEndowView): ChoosePromotionFragment =
            ChoosePromotionFragment().apply {
                initialMyOffers = endowView.myVouchers
                initialOtherOffers = endowView.otherVouchers
                initialMyIsLastPage = endowView.myIsLastPage
                initialOtherIsLastPage = endowView.otherIsLastPage
                // Pre-select TẤT CẢ ưu đãi đang áp (kể cả đang UNAVAILABLE) để user thấy & bỏ chọn
                // được — khớp iOS (`appliedDiscounts.map { $0.objectId }`, không lọc `valid`).
                preSelectedVoucherIds = endowView.discountDetails
                    .map { it.objectId }
                    .toSet()
                onApplySelectedOffers = { offers, onSettled ->
                    endowView.applySelectedOffers(offers, onSettled)
                }
            }
    }
}
