package com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.ui.di.promotionViewModelFactory
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.databinding.FragmentChoosePromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ChoosePromotionListItem
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ChoosePromotionMainAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.utils.extension.VerticalSpaceItemDecoration

class ChoosePromotionFragment : PRMBaseFragment<FragmentChoosePromotionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChoosePromotionBinding.inflate(inflater, container, false)

    // ─── Input từ host ────────────────────────────────────────────────────────

    /**
     * Data đã load sẵn từ [PRMEndowView] — truyền vào để tránh double API call.
     * Nếu không truyền (rỗng) thì ViewModel sẽ tự gọi API.
     */
    var initialMyOffers: List<EligibleOffer> = emptyList()
    var initialOtherOffers: List<EligibleOffer> = emptyList()

    /**
     * Set objectId của các voucher cần pre-select (valid=true từ discountDetails trước đó).
     * Host truyền vào: endowView.discountDetails.filter { it.valid }.map { it.objectId }.toSet()
     */
    var preSelectedVoucherIds: Set<String> = emptySet()

    /**
     * Callback trả về [AppliedDiscount] từ validateStackableDiscounts mới cho host.
     * Host nhận → gọi [PRMEndowView.setDiscountDetails].
     */
    var onApplyVoucher: ((List<AppliedDiscount>) -> Unit)? = null

    // ─── Internal state ───────────────────────────────────────────────────────

    private val isMultiSelection = false
    private val currentSelectedVouchers = mutableListOf<MyVoucherListItem>()
    private var isMyVoucherExpanded = false
    private lateinit var mainAdapter: ChoosePromotionMainAdapter

    private val viewModelFactory by lazy { promotionViewModelFactory() }
    private val viewModel: ChoosePromotionViewModel by viewModels { viewModelFactory }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun setupUI() {
        setupRecyclerView()
        setupButtons()
        setupSearch()
    }

    override fun observeData() {
        super.observeData()

        collectFlow(viewModel.uiState) { state ->
            binding.shimmerProvider.root.isVisible = state.isLoading
            binding.btnApply.isEnabled = !state.isValidating
            binding.btnApply.alpha = if (state.isValidating) 0.5f else 1f

            // Sync currentSelectedVouchers từ preSelectedVoucherIds khi data load xong lần đầu
            if (state.hasLoadedInitial && currentSelectedVouchers.isEmpty() && preSelectedVoucherIds.isNotEmpty()) {
                val allVouchers = state.vouchers + state.otherVouchers
                currentSelectedVouchers.addAll(allVouchers.filter { it.voucherId in preSelectedVoucherIds })
            }

            rebuildList(state)
        }

        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is ChoosePromotionEffect.ShowError ->
                    showToast(mapErrorMessage(effect.errorCode))

                is ChoosePromotionEffect.OpenVoucherDetail -> {
                    openPromotionDetail(effect.voucherId)
                }

                // validateStackableDiscounts thành công → trả AppliedDiscount về host rồi back
                is ChoosePromotionEffect.ApplyValidatedVouchers -> {
                    if (effect.details.isEmpty()) {
                        return@collectFlow
                    }
                    onApplyVoucher?.invoke(effect.details)
                    onBackFragment()
                }
            }
        }

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
            onVoucherClick = { handleVoucherSelection(it) },
            onDetailClick  = { openPromotionDetail(it.voucherId) },
            onSeeMoreMyVoucher = {
                val state = viewModel.uiState.value
                if (!isMyVoucherExpanded) {
                    isMyVoucherExpanded = true
                    rebuildList(state)
                } else if (!state.isLastPage) {
                    viewModel.handleAction(ChoosePromotionAction.LoadMoreMyVouchers)
                }
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

        if (state.vouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_my_endow)))
            val visible = if (isMyVoucherExpanded) state.vouchers else state.vouchers.take(COLLAPSED_COUNT)
            visible.forEach { voucher ->
                val isSelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
                items.add(ChoosePromotionListItem.VoucherItem(voucher.copy(isSelected = isSelected)))
            }
            if (state.vouchers.size > COLLAPSED_COUNT) {
                items.add(
                    ChoosePromotionListItem.SeeMoreMyVoucher(
                        isExpanded = isMyVoucherExpanded && !state.isLastPage
                    )
                )
            }
        }

        if (state.otherVouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_endow_different)))
            state.otherVouchers.forEach { voucher ->
                val isSelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
                items.add(ChoosePromotionListItem.VoucherItem(voucher.copy(isSelected = isSelected)))
            }
        }

        mainAdapter.submitList(items)
    }

    // ─── Selection ────────────────────────────────────────────────────────────

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

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { onBackFragment() }
        binding.btnApply.setOnClickListener { onApplyClicked() }
        updateApplyButtonState()
    }

    private fun onApplyClicked() {
        viewModel.handleAction(
            ChoosePromotionAction.ValidateAndApply(selected = currentSelectedVouchers.toList())
        )
    }

    private fun updateApplyButtonState() {
        if (!isMultiSelection) {
            binding.layoutReducePrice.isVisible = false
            return
        }
        val hasSelected = currentSelectedVouchers.isNotEmpty()
        binding.layoutReducePrice.isVisible = hasSelected
        if (!hasSelected) return
        binding.txtNumberChooseEndow.text = getString(R.string.prm_selected_voucher_count, currentSelectedVouchers.size)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /** Cùng khuôn với `SearchMyPromotionFragment`: gõ mỗi ký tự → `QueryChanged` (ViewModel tự debounce). */
    private fun setupSearch() {
        binding.edtVoucher.apply {
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

    private fun mapErrorMessage(error: String) = when (error) {
        ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
        ErrorCodes.NO_RESULT -> getString(R.string.prm_no_result)
        else                  -> getString(R.string.prm_error_general)
    }

    companion object {
        private const val COLLAPSED_COUNT = 2
    }
}
