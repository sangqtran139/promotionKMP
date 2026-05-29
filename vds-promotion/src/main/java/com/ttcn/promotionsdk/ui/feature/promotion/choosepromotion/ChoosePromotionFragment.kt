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

class ChoosePromotionFragment : PRMBaseFragment<FragmentChoosePromotionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChoosePromotionBinding.inflate(inflater, container, false)
    var initialMyVouchers: List<MyVoucherListItem> = emptyList()
    var initialOtherVouchers: List<MyVoucherListItem> = emptyList()
    var selectedVouchers: List<MyVoucherListItem> = emptyList()
    var onApplyVoucher: ((List<MyVoucherListItem>) -> Unit)? = null

    private val isMultiSelection = false
    private val currentSelectedVouchers = mutableListOf<MyVoucherListItem>()
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
            onDetailClick = { addFragment(PromotionDetailFragment()) },
            onSeeMoreMyVoucher = {
                if (isMyVoucherExpanded) {
                    isMyVoucherExpanded = true
                    viewModel.handleAction(ChoosePromotionAction.LoadMoreMyVouchers)
                } else {
                    isMyVoucherExpanded = true
                    rebuildList(viewModel.uiState.value)
                }
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
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                    val shouldLoadMore = lastVisibleItem >= totalItemCount - 2

                    if (shouldLoadMore) {
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
            val myList = if (isMyVoucherExpanded) state.vouchers else state.vouchers.take(3)
            items.addAll(
                myList.map { voucher ->
                    val isSelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
                    ChoosePromotionListItem.VoucherItem(voucher.copy(isSelected = isSelected))
                }
            )
            items.add(ChoosePromotionListItem.SeeMoreMyVoucher)
        }

        if (state.otherVouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prn_endow_differebt)))
            items.addAll(
                state.otherVouchers.map { voucher ->
                    val isSelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
                    ChoosePromotionListItem.VoucherItem(voucher.copy(isSelected = isSelected))
                }
            )
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
        val isCurrentlySelected = currentSelectedVouchers.any { it.voucherId == voucher.voucherId }
        if (isCurrentlySelected) {
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
        val hasSelectedVoucher = currentSelectedVouchers.isNotEmpty()
        binding.layoutReducePrice.isVisible = hasSelectedVoucher
        if (!hasSelectedVoucher) return
        binding.txtNumberChooseEndow.text = "Đã chọn ${currentSelectedVouchers.size} voucher"
        // TODO: binding.txtReducedPrice.text = "-${formatMoney(totalDiscount)}đ"
    }

    private fun formatMoney(amount: Long): String =
        NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount)

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
}