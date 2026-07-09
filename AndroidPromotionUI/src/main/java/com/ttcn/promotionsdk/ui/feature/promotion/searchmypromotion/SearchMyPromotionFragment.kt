package com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.di.promotionViewModelFactory
import com.ttcn.promotionsdk.databinding.FragmentSearchMyPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.MyPromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.buildPromotionListItems
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.ui.utils.extension.hideSoftInput

class SearchMyPromotionFragment : PRMBaseFragment<FragmentSearchMyPromotionBinding>() {

    private val viewModelFactory by lazy { promotionViewModelFactory() }

    private val viewModel: SearchMyPromotionViewModel by viewModels {
        viewModelFactory
    }

    private val searchListAdapter = MyPromotionAdapter(
        onVoucherClick = { voucher, _ ->
            openPromotionDetail(voucher.voucherId)
        },
        onUseClick = { _, _ -> },
    )

    private var latestState: SearchMyPromotionUiState = SearchMyPromotionUiState()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.imgBack.setOnClickListener {
            requireActivity().hideSoftInput()
            onBackFragment()
        }

        binding.sfEndow.onTextChangeListener = { keyword ->
            viewModel.handleAction(SearchMyPromotionAction.QueryChanged(keyword))
        }
        binding.sfEndow.setOnDoneKeyboardListener {
            viewModel.handleAction(SearchMyPromotionAction.Search)
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
                            viewModel.handleAction(SearchMyPromotionAction.LoadMore)
                        }
                    }
                },
            )
        }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            latestState = state
            renderState(state)
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is SearchMyPromotionEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
            }
        }

        binding.sfEndow.getInputField().requestFocus()
    }

    private fun renderState(state: SearchMyPromotionUiState) {
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
                vouchers = state.vouchers,
                isLoadingMore = state.isLoadingMore,
                keyword = state.keyword.trim(),
            ),
        )
    }

    private fun mapErrorMessage(errorCode: String): String {
        return when (errorCode) {
            ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
            else -> getString(R.string.prm_error_general)
        }
    }

    private companion object {
        private const val MIN_KEYWORD_LENGTH = 1
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
