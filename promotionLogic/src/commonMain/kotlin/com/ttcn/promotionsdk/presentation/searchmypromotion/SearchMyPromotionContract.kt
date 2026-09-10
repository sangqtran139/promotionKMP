package com.ttcn.promotionsdk.presentation.searchmypromotion

import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionVoucher

/**
 * Contract của màn "SearchMyPromotion" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [SearchMyPromotionStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
public data class SearchMyPromotionState(
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

/**
 * Hiện view "không tìm thấy kết quả" thay cho list.
 *
 * Hai nền tảng từng phát biểu luật này **khác nhau**: Android
 * (`SearchMyPromotionFragment.renderState`) đòi `keyword.trim().length >= MIN_KEYWORD_LENGTH &&
 * !isLoading && isEmpty`, còn iOS (`SearchMyPromotionViewController.render`) chỉ xét `isEmpty` và
 * mượn `!isLoading` từ nhánh `if` bao ngoài. Chúng đang **ra cùng kết quả** — nhưng chỉ vì
 * [SearchMyPromotionStore] đã set `isEmpty = false` khi xoá từ khoá; sửa chỗ đó một cái là hai bên
 * lệch âm thầm. Nay là một luật, ở một nơi.
 */
public fun SearchMyPromotionState.showsNoResult(): Boolean =
    keyword.isNotBlank() && !isLoading && isEmpty

/** Hiện danh sách kết quả (kèm tiêu đề "Kết quả tìm kiếm"): có từ khoá và có ít nhất một voucher. */
public fun SearchMyPromotionState.showsResults(): Boolean =
    keyword.isNotBlank() && vouchers.isNotEmpty()

/**
 * Voucher theo id trong danh sách đang hiển thị — cho điều hướng sang Chi tiết và cho bottom sheet
 * "Chọn dịch vụ". Bên iOS hàm này từng được chép nguyên văn ở **hai** VM (`MyPromotionViewModel`,
 * `SearchMyPromotionViewModel`).
 */
public fun SearchMyPromotionState.voucher(id: String): MyPromotionVoucher? =
    vouchers.firstOrNull { it.source.voucherId == id }

public sealed interface SearchMyPromotionIntent {
    public data class QueryChanged(val keyword: String) : SearchMyPromotionIntent
    public data object Search : SearchMyPromotionIntent
    public data object LoadMore : SearchMyPromotionIntent
    public data object ClearKeyword : SearchMyPromotionIntent
    public data object Retry : SearchMyPromotionIntent
    public data object ConsumeError : SearchMyPromotionIntent
}
