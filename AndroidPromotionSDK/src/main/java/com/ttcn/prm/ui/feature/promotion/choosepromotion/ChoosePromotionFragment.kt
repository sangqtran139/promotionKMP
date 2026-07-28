package com.ttcn.prm.ui.feature.promotion.choosepromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.prm.ui.di.promotionViewModelFactory
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.presentation.PROMOTION_SEARCH_MAX_LENGTH
import com.ttcn.promotionsdk.presentation.choosepromotion.COLLAPSED_MY_COUNT
import com.ttcn.promotionsdk.presentation.choosepromotion.ChooseSeeMoreState
import com.ttcn.prm.databinding.FragmentChoosePromotionBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.ui.di.PromotionViewModelFactory
import com.ttcn.prm.ui.feature.promotion.choosepromotion.adapter.ChoosePromotionListItem
import com.ttcn.prm.ui.feature.promotion.choosepromotion.adapter.ChoosePromotionMainAdapter
import com.ttcn.prm.ui.feature.promotion.endowview.PRMEndowView
import com.ttcn.prm.ui.feature.promotion.promotiondetail.PromotionDetailEntry
import com.ttcn.prm.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import com.ttcn.prm.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.utils.extension.VerticalSpaceItemDecoration

class ChoosePromotionFragment : PRMBaseFragment<FragmentChoosePromotionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChoosePromotionBinding.inflate(inflater, container, false)

    // ─── Input ────────────────────────────────────────────────────────────────

    /**
     * Data đã load sẵn từ [PRMEndowView] — dùng lại để tránh double API call.
     * Rỗng thì ViewModel tự gọi API.
     *
     * `internal`: [EligibleOffer] thuộc `promotionLogic`. Host dựng màn qua [forEndowView].
     */
    internal var initialMyOffers: List<EligibleOffer> = emptyList()
    internal var initialOtherOffers: List<EligibleOffer> = emptyList()

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

    private val viewModelFactory by lazy { promotionViewModelFactory() }
    private val viewModel: ChoosePromotionViewModel by viewModels { viewModelFactory }

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
            viewModel.handleAction(ChoosePromotionAction.SetPreSelected(listOf(voucherId)))
        }
    }

    override fun observeData() {
        super.observeData()

        collectFlow(viewModel.uiState) { state ->
            binding.shimmerProvider.root.isVisible = state.isLoading

            updateApplyButtonState(state)
            rebuildList(state)
        }

        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is ChoosePromotionEffect.ShowError ->
                    showToast(mapPromotionError(effect.errorCode))

                is ChoosePromotionEffect.OpenVoucherDetail -> {
                    openPromotionDetail(effect.voucherId, PromotionDetailEntry.CHECKOUT)
                }

                // Bấm "Áp dụng" → trả offers đang chọn cho widget (EndowStore validate).
                // Lỗi → KHÔNG áp; ở lại màn chọn + báo lỗi. Thành công → đóng màn. (Giống iOS.)
                is ChoosePromotionEffect.ApplySelectedOffers -> {
                    if (effect.offers.isEmpty()) {
                        return@collectFlow
                    }
                    val apply = onApplySelectedOffers
                    if (apply == null) {
                        onBackFragment()
                        return@collectFlow
                    }
                    apply(effect.offers) { errorCode ->
                        if (errorCode != null) showToast(mapPromotionError(errorCode))
                        else onBackFragment()
                    }
                }
            }
        }

        // Seed voucher pre-select vào store trước khi load (store giữ selection).
        viewModel.handleAction(ChoosePromotionAction.SetPreSelected(preSelectedVoucherIds.toList()))

        // Truyền data đã load sẵn; nếu rỗng → ViewModel tự gọi API
        viewModel.handleAction(
            ChoosePromotionAction.PreloadVouchers(
                myOffers = initialMyOffers,
                otherOffers = initialOtherOffers,
            )
        )
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        mainAdapter = ChoosePromotionMainAdapter(
            onVoucherClick = { viewModel.handleAction(ChoosePromotionAction.ToggleSelection(it.voucherId)) },
            onDetailClick  = { openPromotionDetail(it.voucherId, PromotionDetailEntry.CHECKOUT) },
            // Store chạy state-machine "mở hết → tải trang kế → thu gọn" (gộp cả expand & collapse).
            onSeeMoreMyVoucher = { viewModel.handleAction(ChoosePromotionAction.SeeMoreMy) },
            onCollapseMyVoucher = { viewModel.handleAction(ChoosePromotionAction.SeeMoreMy) },
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
                        viewModel.handleAction(ChoosePromotionAction.LoadMoreOtherVouchers)
                    }
                }
            })
        }
    }

    private fun rebuildList(state: ChoosePromotionUiState) {
        val items = mutableListOf<ChoosePromotionListItem>()
        // Từ khoá đang tìm → tô đỏ đoạn khớp trên item (đối ứng `highlightKeyword` bên iOS).
        val highlightKeyword = state.keyword.trim()

        if (state.vouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_my_endow)))
            val visible = if (state.myExpanded) state.vouchers else state.vouchers.take(COLLAPSED_MY_COUNT)
            visible.forEach { voucher ->
                val isSelected = voucher.voucherId in state.selectedIds
                items.add(
                    ChoosePromotionListItem.VoucherItem(
                        data = voucher.copy(isSelected = isSelected),
                        highlightKeyword = highlightKeyword,
                    )
                )
            }
            // Nút "Xem thêm/Thu gọn": trạng thái tính bằng rule dùng chung ở store (state.mySeeMore).
            if (state.mySeeMore != ChooseSeeMoreState.HIDDEN) {
                items.add(
                    ChoosePromotionListItem.SeeMoreMyVoucher(
                        isExpanded = state.mySeeMore == ChooseSeeMoreState.COLLAPSE
                    )
                )
            }
        }

        if (state.otherVouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_endow_different)))
            state.otherVouchers.forEach { voucher ->
                val isSelected = voucher.voucherId in state.selectedIds
                items.add(
                    ChoosePromotionListItem.VoucherItem(
                        data = voucher.copy(isSelected = isSelected),
                        highlightKeyword = highlightKeyword,
                    )
                )
            }
        }

        mainAdapter.submitList(items)
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { onBackFragment() }
        binding.btnApply.setOnClickListener { onApplyClicked() }
    }

    private fun onApplyClicked() {
        viewModel.handleAction(ChoosePromotionAction.ValidateAndApply)
    }

    /** Thanh "giảm giá" (chỉ hiện ở chế độ multi-select) — selection lấy từ state store. */
    private fun updateApplyButtonState(state: ChoosePromotionUiState) {
        val show = state.isMultiSelection && state.selectedIds.isNotEmpty()
        binding.layoutReducePrice.isVisible = show
        if (show) {
            binding.txtNumberChooseEndow.text =
                getString(R.string.prm_selected_voucher_count, state.selectedIds.size)
        }
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /** Cùng khuôn với `SearchMyPromotionFragment`: gõ mỗi ký tự → `QueryChanged` (ViewModel tự debounce). */
    private fun setupSearch() {
        binding.edtVoucher.apply {
            // Chặn nhập quá giới hạn (TLNV MOB_004 control 2.1 — maxlength 255).
            maxLength = PROMOTION_SEARCH_MAX_LENGTH
            onTextChangeListener = { keyword ->
                if (keyword.isEmpty()) {
                    viewModel.handleAction(ChoosePromotionAction.ClearKeyword)
                } else {
                    viewModel.handleAction(ChoosePromotionAction.QueryChanged(keyword))
                }
            }
            setOnSearchActionListener { viewModel.handleAction(ChoosePromotionAction.Search) }
            setOnDoneKeyboardListener { viewModel.handleAction(ChoosePromotionAction.Search) }
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
         * ```kotlin
         * binding.endowView.onOpenVoucherSelection = {
         *     addFragment(ChoosePromotionFragment.forEndowView(binding.endowView))
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
