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
import com.ttcn.prm.ui.feature.choosepromotion.adapter.ChoosePromotionListItem
import com.ttcn.prm.ui.feature.choosepromotion.adapter.ChoosePromotionMainAdapter
import com.ttcn.prm.ui.feature.ext.toVoucherListItem
import com.ttcn.promotionsdk.presentation.base.PRMEffect
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionIntent
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionState
import com.ttcn.promotionsdk.presentation.choosepromotion.canApply
import com.ttcn.promotionsdk.presentation.choosepromotion.highlightKeyword
import com.ttcn.promotionsdk.presentation.choosepromotion.isSelected
import com.ttcn.promotionsdk.presentation.choosepromotion.mySeeMoreState
import com.ttcn.promotionsdk.presentation.choosepromotion.shouldLoadMoreOther
import com.ttcn.promotionsdk.presentation.choosepromotion.showsEmptyView
import com.ttcn.promotionsdk.presentation.choosepromotion.showsNoResult
import com.ttcn.promotionsdk.presentation.choosepromotion.showsSelectedCount
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
        // Container SDK nhận được có tràn xuống dưới navigation bar hay không phụ thuộc host —
        // xem `applyNavigationBarInset`. Cùng cơ chế `PromotionDetailFragment` đang dùng cho `tvUse`.
        //
        // Áp cho `ctlApplyVoucher` (CardView bọc ngoài), KHÔNG phải `btnApply`: khác với `tvUse` —
        // con trực tiếp của root, chỉ neo bottom đơn giản — `btnApply` nằm trong 1
        // `ConstraintLayout` con `match_parent` lồng bên trong `CardView` `wrap_content`. Cộng
        // `bottomMargin` cho `btnApply` không kéo theo CardView bọc ngoài (nền trắng + đổ bóng) di
        // chuyển đúng, nên nav bar vẫn đè lên. `ctlApplyVoucher` mới là view neo bottom trực tiếp vào
        // root — tương đương `tvUse` — nên phải là view nhận margin.
        applyNavigationBarInset(binding.ctlApplyVoucher)
        setupRecyclerView()
        setupPullToRefresh()
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
            // Shimmer hiện cho CẢ hai kiểu nạp: mở màn (`isLoading`) và kéo-để-tải-lại
            // (`isRefreshing`). Store cố ý tách hai cờ, nhưng ở đây UI muốn cùng một hiệu ứng —
            // người dùng kéo xong phải thấy màn đang dựng lại, không phải danh sách cũ đứng im.
            val showShimmer = state.isLoading || state.isRefreshing

            // Phải ẩn list khi shimmer hiện: trong `fragment_choose_promotion.xml`, `shimmer_provider`
            // là con ĐẦU TIÊN còn `rcvVoucher` là con sau nó, mà RecyclerView có nền đục
            // (`@color/prm_color_f4f4f4`) nên vẽ đè kín shimmer. Đối ứng `homeList.isVisible` ở
            // `MyPromotionFragment`.
            binding.shimmerProvider.root.isVisible = showShimmer

            // Gõ từ khoá mà không ra gì → view "không tìm thấy" thay cho list, giống màn "Tìm ưu đãi"
            // (`SearchMyPromotionFragment.renderState`). Luật ở store — iOS đọc cùng hàm.
            // Lúc shimmer đang hiện thì KHÔNG cho "không tìm thấy" chen vào: refresh ra rỗng sẽ nháy
            // empty-view một nhịp trước khi dữ liệu mới về.
            val showNoResult = state.showsEmptyView() && !showShimmer
            binding.ctlNoResult.isVisible = showNoResult
            binding.rcvVoucher.isVisible = !showShimmer && !showNoResult

            // Ẩn cả thanh "Áp dụng" trong lúc shimmer — khớp iOS, nơi shimmer bám bounds của table
            // (chạy tới tận đáy màn) nên phủ luôn vùng nút.
            //
            // Ẩn view chứ không trông vào z-order: shimmer là con ĐẦU TIÊN của `ctlMain`, còn
            // `ctlApplyVoucher` là CardView đứng sau **và có elevation** — nó luôn vẽ đè lên shimmer
            // dù shimmer đã kéo dài tới `parent` bottom.
            binding.ctlApplyVoucher.isVisible = !showShimmer

            updateApplyButtonState(state)
            rebuildList(state)
        }

        collectFlow(viewModel.effects) { effect ->
            when (effect) {
                // Popup (`PRMBaseConfirmDialog`) chứ không toast: kéo-để-tải-lại mà API hỏng thì user
                // đang chủ động chờ kết quả, im lặng là không chấp nhận được. Đối ứng iOS.
                is PRMEffect.ShowError -> showErrorDialog(mapPromotionError(effect.errorCode))
            }
        }

        // `SeedOnce`, KHÔNG phải `SetPreSelected` + `Preload`: `observeData()` chạy lại mỗi lần view
        // được dựng lại, mà store sống lâu hơn view — bắn lại là ghi đè tick của user về bộ đã áp ban
        // đầu và rewind `otherOffers` về trang đầu. Cờ gác nằm ở store nên iOS dùng chung.
        viewModel.dispatch(
            ChoosePromotionIntent.SeedOnce(
                preSelectedIds = preSelectedVoucherIds.toList(),
                myOffers = initialMyOffers,
                otherOffers = initialOtherOffers,
                myIsLastPage = initialMyIsLastPage,
                otherIsLastPage = initialOtherIsLastPage,
            )
        )
    }

    // ─── RecyclerView ─────────────────────────────────────────────────────────

    /**
     * Kéo-để-tải-lại. Store dùng chung đã có sẵn `Refresh` (nạp lại **giữ nguyên** từ khoá và tab,
     * khác `LoadInitial` ở chỗ bật `isRefreshing` thay vì `isLoading` — nên shimmer toàn màn không
     * nhảy ra, chỉ có vòng xoay của SwipeRefreshLayout).
     *
     * Cùng khuôn với `MyPromotionFragment.setupObservers`.
     */
    private fun setupPullToRefresh() {
        binding.swipeRefreshVoucher.setOnRefreshListener {
            viewModel.dispatch(ChoosePromotionIntent.Refresh)
            // Thu vòng xoay lại NGAY: `SwipeRefreshLayout` ở đây chỉ đóng vai **nhận cử chỉ kéo**,
            // còn việc báo "đang tải" đã có shimmer phủ kín màn lo. Để cả hai cùng chạy thì màn có
            // hai chỉ báo chồng nhau cho cùng một lượt nạp.
            //
            // `isRefreshing = false` KHÔNG huỷ lượt nạp — intent đã dispatch ở dòng trên, store vẫn
            // chạy tiếp; nó chỉ gỡ cái vòng tròn của widget. Nhờ vậy cũng khỏi phải đồng bộ vòng xoay
            // theo `state.isRefreshing` nữa (trước đây phải cẩn thận để lỗi/chậm thì nó còn quay).
            binding.swipeRefreshVoucher.isRefreshing = false
        }
    }

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
                    // Ngưỡng do `shouldLoadMoreOther` (dùng chung) quyết định, tính theo **chỉ số
                    // trong nhóm "Ưu đãi khác"** — không phải vị trí trên toàn list (vốn còn header,
                    // divider, hàng "Xem thêm"). Bỏ luôn `dy <= 0`: list ngắn hơn màn hình thì không
                    // có cú cuộn nào, bản cũ không bao giờ nạp thêm được còn iOS thì có.
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisible = lm.findLastVisibleItemPosition()
                    if (lastVisible == RecyclerView.NO_POSITION) return
                    val indexInOther = lastVisible - otherSectionOffset
                    if (viewModel.state.value.shouldLoadMoreOther(indexInOther)) {
                        viewModel.dispatch(ChoosePromotionIntent.LoadMoreOtherVouchers)
                    }
                }
            })
        }
    }

    /**
     * Vị trí bắt đầu nhóm "Ưu đãi khác" trên list phẳng — đặt lại mỗi lần [rebuildList].
     * `MAX_VALUE` khi chưa có nhóm đó: mọi chỉ số quy đổi sẽ âm nên không kích hoạt nạp thêm.
     */
    private var otherSectionOffset: Int = Int.MAX_VALUE

    private fun rebuildList(state: ChoosePromotionState) {
        val items = mutableListOf<ChoosePromotionListItem>()
        otherSectionOffset = Int.MAX_VALUE
        // Từ khoá đang tìm → tô đỏ đoạn khớp trên item (đối ứng `highlightKeyword` bên iOS).
        val highlightKeyword = state.highlightKeyword()

        if (state.myOffers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_my_endow)))
            state.visibleMyOffers().forEach { offer ->
                val voucher = offer.toVoucherListItem()
                val isSelected = state.isSelected(voucher.voucherId)
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
            // Vị trí item ĐẦU TIÊN của nhóm "Ưu đãi khác" trên list phẳng. Scroll listener trừ đi số
            // này để ra chỉ số TRONG NHÓM, thứ mà rule dùng chung `shouldLoadMoreOther` nhận.
            otherSectionOffset = items.size
            state.otherOffers.forEach { offer ->
                val voucher = offer.toVoucherListItem()
                val isSelected = state.isSelected(voucher.voucherId)
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
        // Chặn spam: `btnApply.isEnabled` bám `canApply()` nhưng chỉ đổi khi state phát ra lượt kế
        // (collect bất đồng bộ), nên vẫn hở một khung hình cho cú bấm thứ hai. Đọc thẳng state ở đây
        // là bịt hẳn. Đối ứng `guard !state.isApplying` bên iOS.
        if (viewModel.state.value.isApplying) return
        val offers = viewModel.selectedOffers()
        if (offers.isEmpty()) return
        val apply = onApplySelectedOffers
        if (apply == null) {
            // Không có widget để áp (fragment bị FragmentManager tái tạo nên mất closure, hoặc host
            // tự dựng màn này không qua `forEndowView`). Báo lỗi rồi Ở LẠI — đóng màn ở đây là nói
            // dối user rằng đã áp xong.
            showErrorDialog(mapPromotionError(ErrorCodes.GENERAL))
            return
        }
        // Khoá nút cho tới khi `validateStackableDiscounts` trả về (`applySelectedOffers` luôn gọi
        // `onSettled` ở mọi nhánh, kể cả widget đã detach — nên không có đường nào kẹt khoá).
        viewModel.dispatch(ChoosePromotionIntent.ApplyStarted)
        apply(offers) { errorCode ->
            viewModel.dispatch(ChoosePromotionIntent.ApplyFinished)
            if (errorCode != null) {
                // Popup chứ không toast: lỗi validate
                // bị nuốt hoàn toàn: user bấm "Áp dụng", API hỏng, màn đứng im không một thông báo.
                // Dùng `showAlways` như PRM_MOB_021: user vừa chủ động bấm và đang chờ kết quả, im
                // lặng là không chấp nhận được. (iOS dùng popup `PRMConfirmationDialog`.)
                showErrorDialog(mapPromotionError(errorCode))
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
        val show = state.showsSelectedCount()
        binding.layoutReducePrice.isVisible = show
        if (show) {
            binding.txtNumberChooseEndow.text =
                getString(R.string.prm_selected_voucher_count, state.selectedIds.size)
        }
        binding.btnApply.isEnabled = state.canApply()
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /** Cùng khuôn với `SearchMyPromotionFragment`: gõ mỗi ký tự → `QueryChanged` (ViewModel tự debounce). */
    private fun setupSearch() {
        binding.edtVoucher.apply {
            // Chặn nhập quá giới hạn (TLNV MOB_004 control 2.1 — maxlength 255).
            maxLength = PROMOTION_SEARCH_MAX_LENGTH
            // Gõ trắng KHÔNG cần rẽ nhánh sang `ClearKeyword`: store đã xử đúng đường đó trong
            // `onQueryChanged` (huỷ debounce, nạp lại ngay). Nhánh cũ ở đây và bản chép của nó bên
            // iOS (`ChoosePromotionViewModel.query(_:)`) là cùng một luật viết hai lần.
            onTextChangeListener = { keyword ->
                viewModel.dispatch(ChoosePromotionIntent.QueryChanged(keyword))
            }
            setOnSearchActionListener { viewModel.dispatch(ChoosePromotionIntent.Search) }
            // Phím Done chỉ ĐÓNG BÀN PHÍM, không gọi lại API.
            //
            // Mỗi ký tự gõ vào đã dispatch `QueryChanged`, và store debounce rồi tự nạp. Đến lúc
            // người dùng với tay bấm Done thì debounce đã bắn xong từ lâu — dispatch thêm `Search`
            // ở đây là gọi `findEligible` lần hai với **đúng từ khoá cũ**, tốn một vòng mạng mà
            // danh sách không đổi gì.
            //
            // Truyền `null` chứ không xoá hẳn lời gọi: `setOnDoneKeyboardListener` là chỗ duy nhất
            // gọi `hideSoftInput()`, bỏ đi thì bàn phím không đóng nữa.
            setOnDoneKeyboardListener(null)
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
         * `internal`: fragment này là UI nội bộ. `PRMEndowView` tự gọi hàm này qua
         * `PromotionSDK.openChoosePromotion(activity, endowView)` khi user bấm widget — host không
         * đụng tới class này, kể cả gián tiếp.
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
