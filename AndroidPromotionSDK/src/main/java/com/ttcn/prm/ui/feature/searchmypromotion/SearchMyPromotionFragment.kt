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
import com.ttcn.promotionsdk.presentation.searchmypromotion.showsNoResult
import com.ttcn.promotionsdk.presentation.searchmypromotion.showsResults
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
        binding.sfOffer.maxLength = PROMOTION_SEARCH_MAX_LENGTH
        binding.sfOffer.onTextChangeListener = { keyword ->
            viewModel.dispatch(SearchMyPromotionIntent.QueryChanged(keyword))
        }
        // Phím Done chỉ ĐÓNG BÀN PHÍM, không gọi lại API.
        //
        // Mỗi ký tự gõ vào đã dispatch `QueryChanged`, store debounce 400ms rồi tự tìm. Đến lúc
        // người dùng với tay bấm Done thì debounce đã bắn xong — dispatch thêm `Search` ở đây là
        // gọi `searchCustomerVouchers` lần hai với **đúng từ khoá cũ**.
        //
        // Truyền `null` chứ không xoá hẳn lời gọi: `setOnDoneKeyboardListener` là chỗ duy nhất gọi
        // `hideSoftInput()`, bỏ đi thì bàn phím không đóng nữa.
        // Đối ứng `ChoosePromotionFragment.setupSearch`.
        binding.sfOffer.setOnDoneKeyboardListener(null)

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
                // KHÔNG hiện gì: SDK đã bỏ toast. Vẫn thu effect để store `ConsumeError` chạy đúng
                // vòng của nó — bỏ luôn `collectFlow` thì `errorCode` nằm lại trong state.
                // Màn này còn empty-view/list cũ nên user vẫn hiểu được chuyện gì xảy ra.
                is PRMEffect.ShowError -> Unit
            }
        }

        binding.sfOffer.getInputField().requestFocus()
    }

    /** Giống hệt `MyPromotionFragment.showServiceSelector` — cùng bottom sheet, cùng sự kiện host. */
    private fun showServiceSelector(voucher: MyVoucherListItem, services: List<ServiceSelectorUiItem>) {
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

    private fun renderState(state: SearchMyPromotionState) {
        // Cả hai quyết định đều là rule dùng chung ở store — iOS đọc đúng hai hàm này.
        val showNoResult = state.showsNoResult()
        val showResults = state.showsResults()
        val hasValidKeyword = state.keyword.isNotBlank()

        binding.shimmerProvider.root.isVisible = state.isLoading && state.vouchers.isEmpty()
        binding.ctlSearch.isVisible = showResults || (hasValidKeyword && state.isLoading)
        binding.tvTitle.isVisible = showResults
        binding.rcvSearchList.isVisible = showResults
        binding.ctlNoResult.isVisible = showNoResult
        binding.imgNoData.isVisible = showNoResult
        binding.tvNoResultSubtext.isVisible = showNoResult

        binding.tvNoResult.text = when {
            showNoResult -> prmString(R.string.prm_search_no_result)
            else -> ""
        }
        binding.tvNoResultSubtext.text = prmString(R.string.prm_discover_voucher)

        searchListAdapter.submitList(
            buildPromotionListItems(
                vouchers = state.vouchers.map { it.toMyVoucherListItem() },
                isLoadingMore = state.isLoadingMore,
                keyword = state.keyword.trim(),
            ),
        )
    }


    private companion object {
        // `MIN_KEYWORD_LENGTH = 1` đã bỏ: nó chỉ là cách viết khác của "từ khoá không rỗng", và luật
        // đó nay nằm ở `SearchMyPromotionState.showsNoResult()/showsResults()` dùng chung với iOS.
        private const val LOAD_MORE_THRESHOLD = 2
    }
}
