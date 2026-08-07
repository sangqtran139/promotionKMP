package com.ttcn.promotionsdk.presentation.searchmypromotion

import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionVoucher

/**
 * Contract của màn "SearchMyPromotion" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [SearchMyPromotionStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
data class SearchMyPromotionState(
    val keyword: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val isLastPage: Boolean = true,
    val page: Int = 0,
    val pageSize: Int = 10,
    val vouchers: List<MyPromotionVoucher> = emptyList(),
    val errorCode: String? = null,
)

sealed interface SearchMyPromotionIntent {
    data class QueryChanged(val keyword: String) : SearchMyPromotionIntent
    data object Search : SearchMyPromotionIntent
    data object LoadMore : SearchMyPromotionIntent
    data object ClearKeyword : SearchMyPromotionIntent
    data object Retry : SearchMyPromotionIntent
    data object ConsumeError : SearchMyPromotionIntent
}
