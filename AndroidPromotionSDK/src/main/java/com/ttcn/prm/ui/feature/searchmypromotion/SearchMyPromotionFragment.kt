package com.ttcn.prm.ui.feature.searchmypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.ttcn.prm.ui.di.promotionViewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.promotionsdk.presentation.common.PROMOTION_SEARCH_MAX_LENGTH
import com.ttcn.prm.databinding.PrmFragmentSearchMyPromotionBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.PromotionServiceSelection
import com.ttcn.prm.ui.feature.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.feature.mypromotion.toMyVoucherListItem
import com.ttcn.promotionsdk.presentation.base.PRMEffect
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionIntent
import com.ttcn.promotionsdk.presentation.searchmypromotion.SearchMyPromotionState
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorBottomSheet
import com.ttcn.prm.ui.feature.mypromotion.ServiceSelectorUiItem
import com.ttcn.prm.ui.feature.mypromotion.adapter.MyPromotionAdapter
import com.ttcn.prm.ui.feature.mypromotion.adapter.buildPromotionListItems
import com.ttcn.prm.ui.utils.extension.hideSoftInput

internal class SearchMyPromotionFragment : PRMBaseFragment<PrmFragmentSearchMyPromotionBinding>() {

    private val viewModel: SearchMyPromotionViewModel by viewModels { promotionViewModelFactory() }

    private val searchListAdapter = MyPromotionAdapter(
        onVoucherClick = { voucher, _ ->
            openPromotionDetail(voucher.voucherId)
        },
        // Bấm "Sử dụng" → bottom sheet chọn dịch vụ, y như màn "Ưu đãi của tôi" và
        // `SearchMyPromotionViewController.myPromotionCellDidTapUse` bên iOS.
        onUseClick = { voucher, _ ->
            showServiceSelector(voucher, viewModel.serviceOptions(voucher))
        },
    )

    private var latestState: SearchMyPromotionState = SearchMyPromotionState()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        PrmFragmentSearchMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener {
            requireActivity().hideSoftInput()
            goBack()
        }

        // Chặn nhập quá giới hạn (TLNV MOB_001 control 5.2 — maxlength 255).
        binding.sfEndow.maxLength = PROMOTION_SEARCH_MAX_LENGTH
        binding.sfEndow.onTextChangeListener = { keyword ->
            viewModel.dispatch(SearchMyPromotionIntent.QueryChanged(keyword))
        }
        binding.sfEndow.setOnDoneKeyboardListener {
            viewModel.dispatch(SearchMyPromotionIntent.Search)
        }

        binding.rcvSearchList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchListAdapter
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy <= 0) return
                        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val lastVisible = lm.findLastVisibleItemPosition()
                        val shouldLoadMore = searchListAdapter.itemCount > 0 &&
                                !latestState.isLoading &&
                                !latestState.isLoadingMore &&
                                !latestState.isLastPage &&
                                lastVisible >= searchListAdapter.itemCount - LOAD_MORE_THRESHOLD
                        if (shouldLoadMore) {
                            viewModel.dispatch(SearchMyPromotionIntent.LoadMore)
                        }
                    }
                },
            )
        }
    }

    override fun observeData() {
        collectFlow(viewModel.state) { state ->
            latestState = state
            renderState(state)
        }
        collectFlow(viewModel.effects) { effect ->
            when (effect) {
                is PRMEffect.ShowError -> showToast(mapPromotionError(effect.errorCode))
            }
        }

        binding.sfEndow.getInputField().requestFocus()
    }

    /** Giống hệt `MyPromotionFragment.showServiceSelector` — cùng bottom sheet, cùng sự kiện host. */
    private fun showServiceSelector(voucher: MyVoucherListItem, services: List<ServiceSelectorUiItem>) {
        ServiceSelectorBottomSheet.present(this, services) { service ->
            // Báo host (đối ứng iOS onServiceSelected) rồi vẫn để VM xử lý điều hướng nội bộ.
            PromotionSDK.getCallback()?.onServiceSelected(
                PromotionServiceSelection(
                    voucherId = voucher.voucherId,
                    serviceCode = service.serviceCode,
                    serviceName = service.serviceName,
                    serviceType = service.serviceType,
                    iconUrl = service.iconUrl,
                )
            )
        }
    }

    private fun renderState(state: SearchMyPromotionState) {
        val trimmedKeyword = state.keyword.trim()
        val hasValidKeyword = trimmedKeyword.length >= MIN_KEYWORD_LENGTH
        val showNoResult = hasValidKeyword &&
                !state.isLoading &&
                state.isEmpty
        val showResults = hasValidKeyword && state.vouchers.isNotEmpty()

        binding.shimmerProvider.root.isVisible = state.isLoading && state.vouchers.isEmpty()
        binding.ctlSearch.isVisible = showResults || (hasValidKeyword && state.isLoading)
        binding.tvTitle.isVisible = showResults
        binding.rcvSearchList.isVisible = showResults
        binding.ctlNoResult.isVisible = showNoResult
        binding.imgNoData.isVisible = showNoResult
        binding.tvNoResultSubtext.isVisible = showNoResult

        binding.tvNoResult.text = when {
            showNoResult -> getString(R.string.prm_search_no_result)
            else -> ""
        }
        binding.tvNoResultSubtext.text = getString(R.string.prm_discover_voucher)

        searchListAdapter.submitList(
            buildPromotionListItems(
                vouchers = state.vouchers.map { it.toMyVoucherListItem() },
                isLoadingMore = state.isLoadingMore,
                keyword = state.keyword.trim(),
            ),
        )
    }


    private companion object {
        private const val MIN_KEYWORD_LENGTH = 1
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
