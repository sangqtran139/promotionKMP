package com.ttcn.prm.ui.feature.mypromotion

import com.ttcn.promotionsdk.domain.model.voucher.ApplicableProduct
import com.ttcn.promotionsdk.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.domain.model.voucher.VoucherStatus
import com.ttcn.promotionsdk.domain.model.voucher.VoucherTabItem
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionTab
import com.ttcn.promotionsdk.presentation.mypromotion.MyPromotionVoucher

/**
 * **Model hiển thị của Android** cho màn "Ưu đãi của tôi" (dùng chung với màn "Tìm ưu đãi").
 *
 * Không phải MVI contract — State/Intent nằm ở `promotionLogic` (`MyPromotionContract.kt`), Fragment
 * đọc thẳng chúng. Ở đây chỉ là cách RecyclerView/adapter muốn nhìn dữ liệu, cộng vài phép map từ
 * model của store sang.
 */
internal data class MyVoucherListItem(
    val voucherId: String,
    val campaignId: String = "",
    val merchantName: String,
    val title: String,
    val description: String,
    val logo: String,
    val expirationDate: String,
    val displayStatusLabel: String,
    val status: VoucherStatus,
    val objectType: String = "CAMPAIGN",
    val isSelected: Boolean = false,
    val isAutoApplied: Boolean = false,
    val applicableProducts: List<ApplicableProduct> = emptyList(),
    /**
     * Voucher còn dùng được — **quyết định do store tính** (`MyPromotionVoucher.isEnabled` /
     * `ChooseOffer.isUsable`). UI đọc thẳng, KHÔNG tự suy lại từ [status] (tránh 2 nền tảng lệch rule).
     */
    val isEnabled: Boolean = true,
    /**
     * Số ngày còn lại khi voucher sắp hết hạn (trong ngưỡng `expireWarningDate` của server) —
     * **quyết định do store tính**; `null` nếu không áp dụng. UI chỉ format "Còn X ngày".
     */
    val expiringInDays: Int? = null,
    /**
     * Hiện dải "Chưa đủ điều kiện áp dụng" (chỉ màn Chọn ưu đãi) — **quyết định do store tính**
     * (`ChooseOffer.showsIneligibleWarning()`). Khác [isEnabled]: voucher HẾT HẠN cũng `isEnabled =
     * false` nhưng KHÔNG hiện dải, vì đó không phải chuyện điều kiện của đơn hàng.
     */
    val showsIneligibleWarning: Boolean = false,
    /**
     * Ưu đãi bị `validateStackableDiscounts` từ chối ở màn "Chọn ưu đãi" (`ChooseOffer.isRejected`).
     *
     * Chỉ để **giấu badge trạng thái**: ca này card chỉ mờ đi, không đeo nhãn nào — lý do đã hiện ở
     * popup. Không có cờ này thì adapter (bật badge theo `!isEnabled`) hiện "Không đủ điều kiện".
     *
     * Mặc định `false` nên hai màn kia (`Ưu đãi của tôi`, `Tìm ưu đãi`) không đổi gì.
     */
    val isRejected: Boolean = false,
)

internal data class TabItem(
    val code: String,
    val label: String,
    val count: Int,
    val order: Int,
    val isDefault: Boolean = false,
)

internal fun VoucherItem.toMyVoucherListItem(): MyVoucherListItem {
    return MyVoucherListItem(
        voucherId = voucherId,
        campaignId = campaignId.orEmpty(),
        merchantName = merchantName.orEmpty(),
        title = title.orEmpty(),
        description = description.orEmpty(),
        logo = logo.orEmpty(),
        expirationDate = expirationDate.orEmpty(),
        displayStatusLabel = displayStatusLabel.orEmpty(),
        status = VoucherStatus.from(status),
        objectType = objectType,
        isAutoApplied = isAutoApplied,
        applicableProducts = applicableProducts,
    )
}

internal fun VoucherTabItem.toMyVoucherTabUi(): TabItem {
    return TabItem(
        code = code,
        label = label,
        count = count ?: 0,
        order = order ?: Int.MAX_VALUE,
        isDefault = isDefault,
    )
}

/** `internal` để dùng chung MyPromotion + ChoosePromotion (cùng model tab UI). */
internal fun MyPromotionTab.toTabItem() = TabItem(
    code = code,
    label = label,
    count = count,
    order = order,
    isDefault = isDefault,
)

/**
 * Map `MyPromotionVoucher` (store, đã tính quyết định) → `MyVoucherListItem` (model UI Android).
 * `internal` để **dùng chung** giữa màn "Ưu đãi của tôi" và "Tìm ưu đãi" (cả hai hiển thị cùng cell).
 */
internal fun MyPromotionVoucher.toMyVoucherListItem() = MyVoucherListItem(
    voucherId = source.voucherId,
    campaignId = source.campaignId.orEmpty(),
    merchantName = source.merchantName.orEmpty(),
    title = source.title.orEmpty(),
    description = source.description.orEmpty(),
    logo = source.logo.orEmpty(),
    expirationDate = source.expirationDate.orEmpty(),
    displayStatusLabel = source.displayStatusLabel.orEmpty(),
    status = VoucherStatus.from(source.status),
    objectType = source.objectType,
    isAutoApplied = source.isAutoApplied,
    applicableProducts = source.applicableProducts,
    // Quyết định hiển thị lấy thẳng từ store (không tự suy lại ở adapter).
    isEnabled = isEnabled,
    expiringInDays = expiringInDays,
)
