package com.ttcn.promotionsdk.ui.feature.promotion.mypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.databinding.FragmentMyPromotionBinding
import com.ttcn.promotionsdk.ui.base.PRMBaseFragment
import com.ttcn.promotionsdk.ui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.ChoosePromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.MyPromotionTabAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.adapter.PromotionListItem
import com.ttcn.promotionsdk.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import com.ttcn.promotionsdk.ui.feature.promotion.searchmypromotion.SearchMyPromotionFragment

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

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnBack.setOnClickListener { onBackFragment() }
        binding.imgSearch.setOnClickListener { openSearchMyPromotion() }
        binding.rvTabs.adapter = tabAdapter

        binding.homeList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeListAdapter
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (dy <= 0) return
                        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val lastVisible = lm.findLastVisibleItemPosition()
                        val shouldLoadMore = homeListAdapter.itemCount > 0 &&
                                !latestState.isLoading &&
                                !latestState.isRefreshing &&
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
            binding.ctlNoResult.isVisible = !state.isLoading && state.isEmpty
            binding.homeList.isVisible = !state.isLoading && state.vouchers.isNotEmpty()

            tabAdapter.submitTabs(state.tabs, state.selectedTabCode)
            homeListAdapter.submitList(state.vouchers.map { PromotionListItem.Endow(it) })
        }
        collectFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is MyPromotionEffect.ShowError -> showToast(mapErrorMessage(effect.errorCode))
                is MyPromotionEffect.OpenVoucherDetail -> Unit
            }
        }
        viewModel.handleAction(MyPromotionAction.LoadInitialIfNeeded)
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
        private const val LOAD_MORE_THRESHOLD = 2
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