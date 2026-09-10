package com.ttcn.promotionsdk.presentation.mypromotion

import com.ttcn.promotionsdk.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.domain.model.voucher.VoucherDisplayState
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.domain.model.voucher.VoucherTabItem
import com.ttcn.promotionsdk.common.daysUntil
import com.ttcn.promotionsdk.presentation.common.ExpiryWarning

/**
 * Contract của màn "MyPromotion" — **State / Intent / model hiển thị**, dùng chung Android & iOS.
 *
 * Tách khỏi [MyPromotionStore] để đọc được "màn này có dữ liệu gì, nhận được lệnh gì" mà không phải
 * lội qua phần điều phối. Logic nằm ở store; ở đây chỉ có cấu trúc, **không** chuỗi hiển thị.
 */
public data class MyPromotionState(
    val hasLoadedInitial: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isRefreshingTab: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val tabs: List<MyPromotionTab> = emptyList(),
    val selectedTabCode: String? = null,
    val keyword: String = "",
    val page: Int = 0,
    val size: Int = 10,
    val isLastPage: Boolean = true,
    val vouchers: List<MyPromotionVoucher> = emptyList(),
    /** Mã lỗi một-lần; native hiển thị rồi `dispatch(ConsumeError)` để xoá. */
    val errorCode: String? = null,
)

/** Nhãn trạng thái đã QUYẾT ĐỊNH ở store — native chỉ tra chuỗi tương ứng, không tự suy. */
public enum class MyPromotionBadge {
    /** Không hiện badge (voucher dùng được, chưa sắp hết hạn). */
    NONE,
    /** Sắp hết hạn — native hiển thị "Còn {expiringInDays} ngày". */
    EXPIRING_SOON,
    USED,
    EXPIRED,
    INELIGIBLE,
}

/** Nút thao tác đã QUYẾT ĐỊNH ở store — native map sang chuỗi + hiện/ẩn. */
public enum class MyPromotionAction {
    /** Hiện nút "Sử dụng". */
    USE,
    /** Không hiện nút. */
    NONE,
}

/**
 * Voucher theo id trong danh sách đang hiển thị — cho điều hướng sang Chi tiết và cho bottom sheet
 * "Chọn dịch vụ". Đối ứng
 * [SearchMyPromotionState.voucher][com.ttcn.promotionsdk.presentation.searchmypromotion.voucher];
 * bên iOS hàm này từng được chép nguyên văn ở cả hai VM.
 */
public fun MyPromotionState.voucher(id: String): MyPromotionVoucher? =
    vouchers.firstOrNull { it.source.voucherId == id }

public sealed interface MyPromotionIntent {
    public data object LoadInitialIfNeeded : MyPromotionIntent
    public data object Refresh : MyPromotionIntent
    public data class SelectTab(val tabCode: String) : MyPromotionIntent
    public data class Search(val keyword: String) : MyPromotionIntent
    public data object LoadMore : MyPromotionIntent
    public data object ConsumeError : MyPromotionIntent
}

public data class MyPromotionTab(
    val code: String,
    val label: String,
    val count: Int,
    val order: Int,
    val isDefault: Boolean = false,
)

/**
 * View-model cho 1 voucher: **bọc** domain [VoucherItem] ([source]) + các **quyết định hiển thị đã tính**
 * ([isEnabled]/[expiringInDays]/[badge]/[action]). Không chép lại field của domain (tránh trùng model),
 * không chứa chuỗi hiển thị — native đọc `source.*` cho dữ liệu thô và enum/số cho phần đã quyết định.
 */
public data class MyPromotionVoucher(
    /** Dữ liệu thô tái dùng từ domain (merchantName/title/logo/expirationDate/status...). */
    val source: VoucherItem,
    // ── Quyết định hiển thị (store tính, native chỉ dùng) ──
    /** Voucher còn dùng được → mở màn/áp; false → hiển thị mờ, không cho thao tác. */
    val isEnabled: Boolean,
    /** Số ngày còn lại khi sắp hết hạn (khi [badge] == EXPIRING_SOON); null nếu không áp dụng. */
    val expiringInDays: Int?,
    val badge: MyPromotionBadge,
    val action: MyPromotionAction,
)

/** [expireWarningDate]: ngưỡng cảnh báo (ngày) từ server — quyết định badge "sắp hết hạn". */
internal fun VoucherItem.toMyPromotionVoucher(expireWarningDate: Int?): MyPromotionVoucher {
    // Ngưỡng chỉ có ở response danh sách; màn Chi tiết cũng cần (TLNV MOB_002 2.4) nên ghi nhớ lại.
    // Idempotent, bỏ qua null — xem [ExpiryWarning].
    ExpiryWarning.remember(expireWarningDate)
    val display = VoucherStatus.from(status).displayState()
    val enabled = display.isUsable
    // "Còn X ngày" chỉ khi còn dùng được và trong ngưỡng [0, expireWarningDate].
    val days = if (enabled && expireWarningDate != null) {
        daysUntil(expirationDate)?.takeIf { it in 0..expireWarningDate }
    } else null
    val badge = when {
        !enabled -> when (display) {
            VoucherDisplayState.USED -> MyPromotionBadge.USED
            VoucherDisplayState.EXPIRED -> MyPromotionBadge.EXPIRED
            else -> MyPromotionBadge.INELIGIBLE
        }
        days != null -> MyPromotionBadge.EXPIRING_SOON
        else -> MyPromotionBadge.NONE
    }
    return MyPromotionVoucher(
        source = this,
        isEnabled = enabled,
        expiringInDays = days,
        badge = badge,
        action = if (enabled) MyPromotionAction.USE else MyPromotionAction.NONE,
    )
}

internal fun VoucherTabItem.toMyPromotionTab() = MyPromotionTab(
    code = code,
    label = label,
    count = count ?: 0,
    order = order ?: Int.MAX_VALUE,
    isDefault = isDefault,
)
