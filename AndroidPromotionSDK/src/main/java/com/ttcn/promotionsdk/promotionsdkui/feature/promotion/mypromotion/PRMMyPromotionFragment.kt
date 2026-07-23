package com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.promotionsdkui.di.promotionViewModelFactory
import com.ttcn.promotionsdk.databinding.FragmentMyPromotionBinding
import com.ttcn.promotionsdk.promotionsdkui.base.PRMBaseFragment
import com.ttcn.promotionsdk.entry.PRMSDK
import com.ttcn.promotionsdk.entry.PRMServiceSelection
import com.ttcn.promotionsdk.promotionsdkui.di.PromotionViewModelFactory
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.adapter.MyPromotionAdapter
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.adapter.MyPromotionTabAdapter
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.adapter.MyPromotionListItem
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.mypromotion.adapter.buildPromotionListItems
import com.ttcn.promotionsdk.promotionsdkui.feature.promotion.searchmypromotion.PRMSearchMyPromotionFragment
import timber.log.Timber

class PRMMyPromotionFragment : PRMBaseFragment<FragmentMyPromotionBinding>() {

    private val viewModelFactory by lazy { promotionViewModelFactory() }

    private val viewModel: MyPromotionViewModel by viewModels {
        viewModelFactory
    }

    private val tabAdapter = MyPromotionTabAdapter(
        onTabSelected = { tab ->
            viewModel.handleAction(MyPromotionAction.SelectTab(tab.code))
        },
    )

    private val homeListAdapter = MyPromotionAdapter(
        onVoucherClick = { voucher, _ ->
            openPromotionDetail(voucher.voucherId)
        },
        onUseClick = { voucher, _ ->
            viewModel.handleAction(MyPromotionAction.OpenServiceSelector(voucher))
        },
    )
    private var latestState: MyPromotionUiState = MyPromotionUiState()
    private var displayedTabCode: String? = null
    private var latestTabs: List<TabItem> = emptyList()
    private var latestSelectedTabCode: String? = null
    private var latestSubmittedItems: List<MyPromotionListItem> = emptyList()

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
            // Empty-view hiện khi tải xong mà rỗng — khớp iOS (`!isLoading && isEmpty`). KHÔNG gác thêm
            // `!isRefreshingTab` (nó nuốt mất empty-view khi tab/refresh trả về rỗng).
            binding.ctlNoResult.isVisible = !state.isLoading && state.isEmpty
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
                if (item is MyPromotionListItem.Endow) {
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
                is MyPromotionEffect.ShowError -> showToast(mapPromotionError(effect.errorCode))
                is MyPromotionEffect.OpenVoucherDetail -> Unit
                is MyPromotionEffect.ShowServiceSelector -> showServiceSelector(
                    voucher = effect.voucher,
                    services = effect.services,
                )
            }
        }
        viewModel.handleAction(MyPromotionAction.LoadInitialIfNeeded)
    }

    private fun submitVoucherItems(
        selectedTabCode: String?,
        items: List<MyPromotionListItem>,
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

    private fun showServiceSelector(voucher: MyVoucherListItem, services: List<ServiceSelectorUiItem>) {
        if (childFragmentManager.findFragmentByTag(ServiceSelectorBottomSheet.TAG) != null) return
        ServiceSelectorBottomSheet.newInstance(
            services = services,
            onServiceSelected = { service ->
                // Báo host (đối ứng iOS onServiceSelected) rồi vẫn để VM xử lý điều hướng nội bộ.
                PRMSDK.getCallback()?.onServiceSelected(
                    PRMServiceSelection(
                        voucherId = voucher.voucherId,
                        serviceCode = service.serviceCode,
                        serviceName = service.serviceName,
                        iconUrl = service.iconUrl,
                    )
                )
                viewModel.handleAction(MyPromotionAction.ServiceSelected(voucher, service))
            },
        ).show(childFragmentManager, ServiceSelectorBottomSheet.TAG)
    }

    private fun openSearchMyPromotion() {
        val fm = requireActivity().supportFragmentManager
        if (fm.findFragmentByTag(TAG_SEARCH_MY_PROMOTION) != null) return
        fm.beginTransaction().setReorderingAllowed(true)
            .add(android.R.id.content, PRMSearchMyPromotionFragment(), TAG_SEARCH_MY_PROMOTION)
            .addToBackStack(TAG_SEARCH_MY_PROMOTION).commit()
    }

    override fun onDestroyView() {
        // Màn bị pop khỏi back stack (user back) → báo host. Đối ứng iOS `vc.onClose → onClosed`.
        // `isRemoving` false khi chỉ đổi cấu hình / đẩy màn khác lên trên (được lưu ở back stack).
        if (isRemoving) PRMSDK.getCallback()?.onClosed()
        super.onDestroyView()
    }

    private companion object {
        private const val TAG_SEARCH_MY_PROMOTION = "prm_search_my_promotion"
        private const val TAG_VOUCHER_DIFF_DEBUG = "VoucherDiffDebug"
        private const val LOAD_MORE_THRESHOLD = 2
    }

}
