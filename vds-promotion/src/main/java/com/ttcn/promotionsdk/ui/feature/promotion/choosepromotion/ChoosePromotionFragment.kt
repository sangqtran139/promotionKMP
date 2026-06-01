package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.databinding.FragmentChoosePromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ChoosePromotionListItem
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ChoosePromotionMainAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import com.ttcn.promotionsdk.ui.utils.extension.VerticalSpaceItemDecoration
import java.text.NumberFormat
import java.util.Locale

/**
 * VERSION A — Load more API bình thường, nhưng cache-aware khi collapse/expand.
 *
 * Flow của "Ưu đãi của tôi":
 *   1. [collapsed]  show 2 item đầu + footer "Xem thêm"
 *   2. Nhấn "Xem thêm":
 *      a. Nếu chưa expand lần nào → expand local (show tất cả đã fetch trong state)
 *         - isLastPage = false → footer "Xem thêm" (còn trang, có thể load more)
 *         - isLastPage = true  → footer "Thu gọn"  (hết data)
 *      b. Nếu đã expanded + isLastPage = false → gọi API LoadMoreMyVouchers
 *         (chỉ gọi khi thực sự còn trang chưa fetch)
 *   3. Nhấn "Thu gọn" → collapsed, reset isMyVoucherExpanded = false
 *   4. Nhấn "Xem thêm" lần 2 sau khi thu gọn:
 *      → chỉ expand local (state vẫn giữ toàn bộ data đã fetch), KHÔNG gọi API
 *      → chỉ gọi API nếu isLastPage = false (tức là chưa fetch hết từ trước)
 *
 * Điểm khác vs Version B:
 *   Version B: sau khi collapse → expand lại luôn gọi API load more
 *   Version A: sau khi collapse → expand lại CHỈ dùng local state;
 *              API chỉ được gọi khi isLastPage = false VÀ đang ở trạng thái expanded
 */
class ChoosePromotionFragment : PRMBaseFragment<FragmentChoosePromotionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChoosePromotionBinding.inflate(inflater, container, false)

    var initialMyVouchers: List<MyVoucherListItem> = emptyList()
    var initialOtherVouchers: List<MyVoucherListItem> = emptyList()
    var selectedVouchers: List<MyVoucherListItem> = emptyList()
    var onApplyVoucher: ((List<MyVoucherListItem>) -> Unit)? = null

    private val isMultiSelection = false
    private val currentSelectedVouchers = mutableListOf<MyVoucherListItem>()

    /**
     * true  = đang hiển thị toàn bộ my-vouchers (expand)
     * false = chỉ hiển thị [COLLAPSED_COUNT] item đầu
     */
    private var isMyVoucherExpanded = false

    private lateinit var mainAdapter: ChoosePromotionMainAdapter

    private val viewModelFactory by inject<PromotionViewModelFactory>()
    private val viewModel: ChoosePromotionViewModel by viewModels { viewModelFactory }

    override fun setupUI() {
        currentSelectedVouchers.clear()
        currentSelectedVouchers.addAll(selectedVouchers.map { it.copy(isSelected = true) })
        setupRecyclerView()
        setupButtons()
        setupSearch()
    }

    override fun observeData() {
        super.observeData()
        collectFlow(viewModel.uiState) { state ->
            binding.shimmerProvider.root.isVisible = state.isLoading
            rebuildList(state)
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is ChoosePromotionEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
                is ChoosePromotionEffect.OpenVoucherDetail -> Unit
            }
        }
        if (initialMyVouchers.isNotEmpty() || initialOtherVouchers.isNotEmpty()) {
            viewModel.handleAction(
                ChoosePromotionAction.InitWithData(
                    myVouchers = initialMyVouchers,
                    otherVouchers = initialOtherVouchers,
                )
            )
        } else {
            viewModel.handleAction(ChoosePromotionAction.LoadInitialIfNeeded)
        }
    }

    private fun setupRecyclerView() {
        mainAdapter = ChoosePromotionMainAdapter(
            onVoucherClick = { handleVoucherSelection(it) },
            onDetailClick = { addFragment(PromotionDetailFragment.newInstance(it.voucherId)) },
            onSeeMoreMyVoucher = {
                val state = viewModel.uiState.value
                if (!isMyVoucherExpanded) {
                    // Lần đầu hoặc sau khi thu gọn: chỉ expand local, không gọi API
                    isMyVoucherExpanded = true
                    rebuildList(state)
                } else if (!state.isLastPage) {
                    // Đang expanded + còn trang chưa fetch → mới gọi API
                    viewModel.handleAction(ChoosePromotionAction.LoadMoreMyVouchers)
                }
                // Đang expanded + isLastPage = true → footer hiển thị "Thu gọn",
                // onSeeMoreMyVoucher không được gọi trong trường hợp này
            },
            onCollapseMyVoucher = {
                isMyVoucherExpanded = false
                rebuildList(viewModel.uiState.value)
            },
        )

        binding.rcvVoucher.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mainAdapter
            addItemDecoration(
                VerticalSpaceItemDecoration(
                    0,
                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)
                )
            )

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    val lastVisible = lm.findLastVisibleItemPosition()
                    val total = lm.itemCount
                    if (lastVisible >= total - 2) {
                        viewModel.handleAction(ChoosePromotionAction.LoadMoreOtherVouchers)
                    }
                }
            })
        }
    }

    /**
     * Quy tắc render footer "Ưu đãi của tôi":
     *
     * | isMyVoucherExpanded | isLastPage | isExpanded (footer) | Nghĩa                              |
     * |---------------------|------------|---------------------|------------------------------------|
     * | false               | any        | false               | "Xem thêm" → expand local          |
     * | true                | false      | false               | "Xem thêm" → load more API         |
     * | true                | true       | true                | "Thu gọn"  → collapse về 2 item    |
     */
    private fun rebuildList(state: ChoosePromotionUiState) {
        val items = mutableListOf<ChoosePromotionListItem>()

        if (state.vouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_my_endow)))

            val visibleMyVouchers =
                if (isMyVoucherExpanded) state.vouchers else state.vouchers.take(COLLAPSED_COUNT)

            visibleMyVouchers.forEach { voucher ->
                val isSelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
                items.add(ChoosePromotionListItem.VoucherItem(voucher.copy(isSelected = isSelected)))
            }

            if (state.vouchers.size > COLLAPSED_COUNT) {
                // isExpanded = true chỉ khi: đang mở rộng VÀ đã hết trang
                // → footer hiển thị "Thu gọn"
                val footerIsExpanded = isMyVoucherExpanded && state.isLastPage
                items.add(ChoosePromotionListItem.SeeMoreMyVoucher(isExpanded = footerIsExpanded))
            }
        }

        if (state.otherVouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prn_endow_differebt)))
            state.otherVouchers.forEach { voucher ->
                val isSelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
                items.add(ChoosePromotionListItem.VoucherItem(voucher.copy(isSelected = isSelected)))
            }
        }

        mainAdapter.submitList(items)
    }

    private fun handleVoucherSelection(voucher: MyVoucherListItem) {
        if (isMultiSelection) handleMultiSelection(voucher) else handleSingleSelection(voucher)
        mainAdapter.updateVoucherSelection(
            voucherId = voucher.voucherId,
            isMultiSelection = isMultiSelection,
        )
        updateApplyButtonState()
    }

    private fun handleSingleSelection(voucher: MyVoucherListItem) {
        val isCurrentlySelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
        currentSelectedVouchers.clear()
        if (!isCurrentlySelected) currentSelectedVouchers.add(voucher)
    }

    private fun handleMultiSelection(voucher: MyVoucherListItem) {
        if (currentSelectedVouchers.any { it.voucherId == voucher.voucherId }) {
            currentSelectedVouchers.removeAll { it.voucherId == voucher.voucherId }
        } else {
            currentSelectedVouchers.add(voucher)
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { onBackFragment() }
        binding.btnApply.setOnClickListener {
            onApplyVoucher?.invoke(currentSelectedVouchers.map { it.copy(isSelected = true) })
            onBackFragment()
        }
        updateApplyButtonState()
    }

    @SuppressLint("SetTextI18n")
    private fun updateApplyButtonState() {
        if (!isMultiSelection) {
            binding.layoutReducePrice.isVisible = false
            return
        }
        val hasSelected = currentSelectedVouchers.isNotEmpty()
        binding.layoutReducePrice.isVisible = hasSelected
        if (!hasSelected) return
        binding.txtNumberChooseEndow.text = "Đã chọn ${currentSelectedVouchers.size} voucher"
    }

    private fun setupSearch() {
        binding.edtVoucher.apply {
            onTextChangeListener = { keyword ->
                if (keyword.isEmpty()) {
                    viewModel.handleAction(ChoosePromotionAction.SearchKeyword(keyword))
                }
            }
            setOnSearchActionListener {
                viewModel.handleAction(
                    ChoosePromotionAction.SearchKeyword(getInputField().text?.toString().orEmpty())
                )
            }
            setOnDoneKeyboardListener {
                viewModel.handleAction(
                    ChoosePromotionAction.SearchKeyword(getInputField().text?.toString().orEmpty())
                )
            }
        }
    }

    private fun mapErrorMessage(error: String): String {
        return when (error) {
            "keyword_too_short" -> getString(R.string.prm_keyword_too_short)
            "missing_customer_id" -> getString(R.string.prm_missing_customer_id)
            "no_result" -> getString(R.string.no_result)
            else -> getString(R.string.prm_error_general)
        }
    }

    companion object {
        private const val COLLAPSED_COUNT = 2
    }
}