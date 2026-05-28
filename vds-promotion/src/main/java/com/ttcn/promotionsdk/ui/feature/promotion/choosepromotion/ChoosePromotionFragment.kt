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
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionFragment
import com.ttcn.promotionsdk.ui.utils.extension.VerticalSpaceItemDecoration
import java.text.NumberFormat
import java.util.Locale

class ChoosePromotionFragment : PRMBaseFragment<FragmentChoosePromotionBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChoosePromotionBinding.inflate(inflater, container, false)

    private val isMultiSelection = true

    private val currentSelectedVouchers = mutableListOf<MyVoucherListItem>()

    private var isMyVoucherExpanded = false
    private var isOtherVoucherExpanded = false

    private lateinit var mainAdapter: ChoosePromotionMainAdapter

    private val viewModelFactory by inject<PromotionViewModelFactory>()
    private val viewModel: ChoosePromotionViewModel by viewModels { viewModelFactory }

    override fun setupUI() {
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
        viewModel.handleAction(ChoosePromotionAction.LoadInitialIfNeeded)
    }

    private fun setupRecyclerView() {
        mainAdapter = ChoosePromotionMainAdapter(
            onVoucherClick = { handleVoucherSelection(it) },
            onDetailClick = { addFragment(PromotionDetailFragment()) },
            onSeeMoreMyVoucher = {
                isMyVoucherExpanded = true
                rebuildList(viewModel.uiState.value)
            },
            onSeeMoreOtherVoucher = {
                isOtherVoucherExpanded = true
                rebuildList(viewModel.uiState.value)
            }
        )

        binding.rcvVoucher.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mainAdapter
            addItemDecoration(VerticalSpaceItemDecoration(0, resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._6sdp)))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!recyclerView.canScrollVertically(1)) {
                        if (isMyVoucherExpanded) viewModel.handleAction(ChoosePromotionAction.LoadMoreMyVouchers)
                        if (isOtherVoucherExpanded) viewModel.handleAction(ChoosePromotionAction.LoadMoreOtherVouchers)
                    }
                }
            })
        }
    }

    private fun rebuildList(state: ChoosePromotionUiState) {
        val items = mutableListOf<ChoosePromotionListItem>()

        // My vouchers section
        if (state.vouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prm_my_endow)))
            val myList = if (isMyVoucherExpanded) state.vouchers else state.vouchers.take(3)
            items.addAll(myList.map { ChoosePromotionListItem.VoucherItem(it) })
            if (!isMyVoucherExpanded && state.vouchers.size > 3) {
                items.add(ChoosePromotionListItem.SeeMoreMyVoucher)
            }
        }

        // Other vouchers section
        if (state.otherVouchers.isNotEmpty()) {
            items.add(ChoosePromotionListItem.SectionHeader(getString(R.string.prn_endow_differebt)))
            val otherList = if (isOtherVoucherExpanded) state.otherVouchers else state.otherVouchers.take(3)
            items.addAll(otherList.map { ChoosePromotionListItem.VoucherItem(it) })
            if (!isOtherVoucherExpanded && state.otherVouchers.size > 3) {
                items.add(ChoosePromotionListItem.SeeMoreOtherVoucher)
            }
        }

        mainAdapter.submitList(items)
    }

    private fun handleVoucherSelection(voucher: MyVoucherListItem) {
        if (isMultiSelection) handleMultiSelection(voucher)
        else handleSingleSelection(voucher)

        mainAdapter.updateVoucherSelection(
            voucherId = voucher.voucherId,
            isMultiSelection = isMultiSelection
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
            viewModel.handleAction(ChoosePromotionAction.ApplyVouchers(currentSelectedVouchers.toList()))
        }
        updateApplyButtonState()
    }

    @SuppressLint("SetTextI18n")
    private fun updateApplyButtonState() {
        val hasSelectedVoucher = currentSelectedVouchers.isNotEmpty()

        if (!isMultiSelection) {
            binding.layoutReducePrice.isVisible = false
            return
        }

        binding.layoutReducePrice.isVisible = hasSelectedVoucher

        if (!hasSelectedVoucher) return

        binding.txtNumberChooseEndow.text = "Đã chọn ${currentSelectedVouchers.size} voucher"
//         TODO: thay bằng field discount thực tế từ MyVoucherListItem
//         binding.txtReducedPrice.text = "-${formatMoney(totalDiscount)}đ"
    }

    private fun formatMoney(amount: Long): String {
        return NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount)
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
}