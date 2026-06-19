package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.databinding.FragmentMyPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.ChoosePromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.MyPromotionTabAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.PromotionListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.buildPromotionListItems
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionFragment
import timber.log.Timber

class MyPromotionFragment : PRMBaseFragment<FragmentMyPromotionBinding>() {

    private val viewModelFactory by inject<PromotionViewModelFactory>()

    private val viewModel: MyPromotionViewModel by viewModels {
        viewModelFactory
    }

    private val tabAdapter = MyPromotionTabAdapter(
        onTabSelected = { tab ->
            viewModel.handleAction(MyPromotionAction.SelectTab(tab.code))
        },
    )

    private val homeListAdapter = ChoosePromotionAdapter(
        onVoucherClick = { voucher, _ ->
            addFragment(PromotionDetailFragment.newInstance(voucher.voucherId))
        },
        onUseClick = { _, _ ->
        },
    )
    private var latestState: MyPromotionUiState = MyPromotionUiState()
    private var displayedTabCode: String? = null
    private var latestTabs: List<TabItem> = emptyList()
    private var latestSelectedTabCode: String? = null
    private var latestSubmittedItems: List<PromotionListItem> = emptyList()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnBack.setOnClickListener { onBackFragment() }
        binding.imgSearch.setOnClickListener { openSearchMyPromotion() }
        binding.rvTabs.adapter = tabAdapter
        binding.rvTabs.itemAnimator = null

        binding.homeList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeListAdapter
            itemAnimator = null
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy <= 0) return
                        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val lastVisible = lm.findLastVisibleItemPosition()
                        val shouldLoadMore = latestState.vouchers.isNotEmpty() &&
                                homeListAdapter.itemCount > 0 &&
                                !latestState.isLoading &&
                                !latestState.isRefreshing &&
                                !latestState.isRefreshingTab &&
                                !latestState.isLoadingMore &&
                                !latestState.isLastPage &&
                                lastVisible >= homeListAdapter.itemCount - LOAD_MORE_THRESHOLD
                        if (shouldLoadMore) {
                            viewModel.handleAction(MyPromotionAction.LoadMore)
                        }
                    }
                },
            )
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.handleAction(MyPromotionAction.Refresh)
        }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            latestState = state
            binding.shimmerProvider.root.isVisible = state.isLoading && state.vouchers.isEmpty()
            binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
            binding.ctlNoResult.isVisible =
                !state.isLoading && !state.isRefreshingTab && state.isEmpty
            binding.homeList.isVisible = state.vouchers.isNotEmpty() ||
                    homeListAdapter.currentList.isNotEmpty()

            val shouldUpdateTabs =
                latestTabs != state.tabs || latestSelectedTabCode != state.selectedTabCode
            if (shouldUpdateTabs) {
                latestTabs = state.tabs
                latestSelectedTabCode = state.selectedTabCode
                tabAdapter.submitTabs(state.tabs, state.selectedTabCode)
            }

            val adapterItems = buildPromotionListItems(
                vouchers = state.vouchers,
                isLoadingMore = state.isLoadingMore,
            )
            adapterItems.forEachIndexed { index, item ->
                if (item is PromotionListItem.Endow) {
                    Timber.tag(TAG_VOUCHER_DIFF_DEBUG)
                        .d("index=$index voucherId=${item.data.voucherId} rowKey=${item.rowKey} title=${item.data.title}")
                }
            }

            val isTabDataLoading =
                state.isLoading &&
                        state.vouchers.isNotEmpty() &&
                        state.selectedTabCode != displayedTabCode
            val shouldSkipEmptyDuringLoading =
                state.isLoading &&
                        state.vouchers.isEmpty() &&
                        homeListAdapter.currentList.isNotEmpty()

            if (!isTabDataLoading && !shouldSkipEmptyDuringLoading) {
                submitVoucherItems(
                    selectedTabCode = state.selectedTabCode,
                    items = adapterItems,
                )
            }
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is MyPromotionEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
                is MyPromotionEffect.OpenVoucherDetail -> Unit
            }
        }
        viewModel.handleAction(MyPromotionAction.LoadInitialIfNeeded)
    }

    private fun submitVoucherItems(
        selectedTabCode: String?,
        items: List<PromotionListItem>,
    ) {
        if (items == latestSubmittedItems) return

        val tabChanged = selectedTabCode != displayedTabCode
        if (tabChanged) {
            displayedTabCode = selectedTabCode
            binding.homeList.scrollToPosition(0)
        }

        latestSubmittedItems = items
        homeListAdapter.submitList(items)
    }

    private fun openSearchMyPromotion() {
        val fm = requireActivity().supportFragmentManager
        if (fm.findFragmentByTag(TAG_SEARCH_MY_PROMOTION) != null) return
        fm.beginTransaction().setReorderingAllowed(true)
            .add(android.R.id.content, SearchMyPromotionFragment(), TAG_SEARCH_MY_PROMOTION)
            .addToBackStack(TAG_SEARCH_MY_PROMOTION).commit()
    }

    private companion object {
        private const val TAG_SEARCH_MY_PROMOTION = "prm_search_my_promotion"
        private const val TAG_VOUCHER_DIFF_DEBUG = "VoucherDiffDebug"
        private const val LOAD_MORE_THRESHOLD = 2
    }

    private fun mapErrorMessage(error: String): String {
        return when (error) {
            ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
            ErrorCodes.NO_RESULT -> getString(R.string.no_result)
            else -> getString(R.string.prm_error_general)
        }
    }
}
