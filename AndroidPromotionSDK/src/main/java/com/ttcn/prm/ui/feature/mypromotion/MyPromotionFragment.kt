package com.ttcn.prm.ui.feature.mypromotion

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.ttcn.prm.ui.di.promotionViewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ttcn.prm.R
import com.ttcn.prm.databinding.PrmFragmentMyPromotionBinding
import com.ttcn.prm.ui.base.PRMBaseFragment
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.PromotionServiceSelection
import com.ttcn.prm.ui.feature.mypromotion.adapter.MyPromotionAdapter
import com.ttcn.prm.ui.feature.mypromotion.adapter.MyPromotionTabAdapter
import com.ttcn.prm.ui.feature.mypromotion.adapter.MyPromotionListItem
import com.ttcn.prm.ui.feature.mypromotion.adapter.buildPromotionListItems
import com.ttcn.prm.ui.feature.searchmypromotion.SearchMyPromotionFragment
import com.ttcn.promotionsdk.presentation.base.PRMEffect
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionIntent
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionState
import com.ttcn.prm.ui.utils.PRMLog

internal class MyPromotionFragment : PRMBaseFragment<PrmFragmentMyPromotionBinding>() {

    private val viewModel: MyPromotionViewModel by viewModels { promotionViewModelFactory() }

    private val tabAdapter = MyPromotionTabAdapter(
        onTabSelected = { tab ->
            viewModel.dispatch(MyPromotionIntent.SelectTab(tab.code))
        },
    )

    private val homeListAdapter = MyPromotionAdapter(
        onVoucherClick = { voucher, _ ->
            openPromotionDetail(voucher.voucherId)
        },
        onUseClick = { voucher, _ ->
            showServiceSelector(voucher, viewModel.serviceOptions(voucher))
        },
    )
    private var latestState: MyPromotionState = MyPromotionState()
    private var displayedTabCode: String? = null
    private var latestTabs: List<TabItem> = emptyList()
    private var latestSelectedTabCode: String? = null
    private var latestShowTabCount: Boolean = false
    private var latestSubmittedItems: List<MyPromotionListItem> = emptyList()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        PrmFragmentMyPromotionBinding.inflate(inflater, container, false)

    override fun setupUI() {
        applyNavigationBarInsetAsScrollPadding(binding.homeList)
        binding.btnBack.setOnClickListener { goBack() }
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
                            viewModel.dispatch(MyPromotionIntent.LoadMore)
                        }
                    }
                },
            )
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.dispatch(MyPromotionIntent.Refresh)
        }
    }

    override fun observeData() {
        collectFlow(viewModel.state) { state ->
            latestState = state
            binding.shimmerProvider.root.isVisible = state.isLoading && state.vouchers.isEmpty()
            binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
            // Empty-view hiện khi tải xong mà rỗng — khớp iOS (`!isLoading && isEmpty`). KHÔNG gác thêm
            // `!isRefreshingTab` (nó nuốt mất empty-view khi tab/refresh trả về rỗng).
            binding.ctlNoResult.isVisible = !state.isLoading && state.isEmpty
            binding.homeList.isVisible = state.vouchers.isNotEmpty() ||
                    homeListAdapter.currentList.isNotEmpty()

            // Số lượng chỉ hiện khi danh sách đã load xong (TLNV MOB_001 control #3) → phải render
            // lại tabs khi cờ loading đổi, không chỉ khi tabs/selected đổi.
            val showTabCount = !state.isLoading
            val tabs = state.tabs.map { it.toTabItem() }
            val shouldUpdateTabs = latestTabs != tabs ||
                    latestSelectedTabCode != state.selectedTabCode ||
                    latestShowTabCount != showTabCount
            if (shouldUpdateTabs) {
                latestTabs = tabs
                latestSelectedTabCode = state.selectedTabCode
                latestShowTabCount = showTabCount
                tabAdapter.submitTabs(tabs, state.selectedTabCode, showTabCount)
            }

            val adapterItems = buildPromotionListItems(
                vouchers = state.vouchers.map { it.toMyVoucherListItem() },
                isLoadingMore = state.isLoadingMore,
            )
            adapterItems.forEachIndexed { index, item ->
                if (item is MyPromotionListItem.Offer) {
                    PRMLog.d(
                        TAG_VOUCHER_DIFF_DEBUG,
                        "index=$index voucherId=${item.data.voucherId} rowKey=${item.rowKey} title=${item.data.title}",
                    )
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
        collectFlow(viewModel.effects) { effect ->
            when (effect) {
                // KHÔNG hiện gì: SDK đã bỏ toast. Vẫn thu effect để store `ConsumeError` chạy đúng
                // vòng của nó — bỏ luôn `collectFlow` thì `errorCode` nằm lại trong state.
                // Màn này còn empty-view/list cũ nên user vẫn hiểu được chuyện gì xảy ra.
                is PRMEffect.ShowError -> Unit
            }
        }
        viewModel.dispatch(MyPromotionIntent.LoadInitialIfNeeded)
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
        // `present` lo luôn: 1 dịch vụ → chọn thẳng không mở sheet, và toast xác nhận. Xem KDoc ở đó.
        ServiceSelectorBottomSheet.present(this, services) { service ->
            // Báo host (đối ứng iOS onServiceSelected) rồi vẫn để VM xử lý điều hướng nội bộ.
            PromotionSDK.getCallback()?.onServiceSelected(
                PromotionServiceSelection(
                    voucherId = voucher.voucherId,
                    productId = service.productId,
                    productName = service.productName,
                    skuSourceId = service.skuSourceId,
                    iconUrl = service.iconUrl,
                )
            )
        }
    }

    /**
     * Mở màn Tìm kiếm **cùng FM và cùng container** với màn này — đi qua [addFragment] của base,
     * giống hệt [openPromotionDetail].
     *
     * Trước đây hàm này tự dựng transaction riêng, chép lại y nguyên phần chọn FM + lấy container +
     * dedup của base. Lý do chọn FM/container như vậy đã dời lên KDoc của [addFragment]; giữ hai bản
     * song song chỉ tạo cơ hội cho chúng trôi lệch nhau.
     */
    private fun openSearchMyPromotion() {
        addFragment(SearchMyPromotionFragment(), tag = TAG_SEARCH_MY_PROMOTION)
    }

    override fun onDestroyView() {
        // Màn bị pop khỏi back stack (user back) → báo host. Đối ứng iOS `vc.onClose → onClosed`.
        // `isRemoving` false khi chỉ đổi cấu hình / đẩy màn khác lên trên (được lưu ở back stack).
        super.onDestroyView()
    }

    private companion object {
        private const val TAG_SEARCH_MY_PROMOTION = "prm_search_my_promotion"
        private const val TAG_VOUCHER_DIFF_DEBUG = "VoucherDiffDebug"
        private const val LOAD_MORE_THRESHOLD = 2
    }

}
