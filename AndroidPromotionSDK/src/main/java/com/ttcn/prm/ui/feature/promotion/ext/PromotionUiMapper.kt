package com.ttcn.prm.ui.feature.promotion.ext

import com.ttcn.promotionsdk.core.config.AvailableService
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherStatus
import com.ttcn.prm.entry.AppliedDiscount
import com.ttcn.prm.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.prm.ui.feature.promotion.mypromotion.ServiceSelectorUiItem

/**
 * Mapper tầng presentation. Ba nhóm:
 *
 *  1. Dựng **domain request** từ model UI / [AppliedDiscount].
 *  2. Diễn giải **kết quả validate → [AppliedDiscount]** per-offer qua `ValidateDiscountsResult`
 *     ([appliedDiscountFor]) cho public surface (callback, [PRMEndowView]).
 *  3. Map **domain model ([EligibleOffer], [AvailableService]) → model UI**.
 *
 * [AppliedDiscount] là model **public** của SDK (ui/entry); domain (use case, repository)
 * chỉ làm việc với domain model, DTO data layer không rò lên đây.
 */

// ─── Build domain request ───────────────────────────────────────────────────

@JvmName("voucherItemsToValidateDiscountsRequest")
internal fun List<MyVoucherListItem>.toValidateDiscountsRequest(
    orderId: String,
    orderValue: String,
): ValidateDiscountsRequest = ValidateDiscountsRequest(
    orderId = orderId,
    orderValue = orderValue,
    items = map { voucher ->
        DiscountItemRequest(objectId = voucher.voucherId, objectType = voucher.objectType)
    },
)

/**
 * Luồng checkout: `objectId` là [EligibleOffer.id] — `voucherId` nếu khách đã sở hữu, ngược lại
 * `campaignId`.
 */
@JvmName("eligibleOffersToValidateDiscountsRequest")
internal fun List<EligibleOffer>.toValidateDiscountsRequest(
    orderId: String,
    orderValue: String,
): ValidateDiscountsRequest = ValidateDiscountsRequest(
    orderId = orderId,
    orderValue = orderValue,
    items = map { offer ->
        DiscountItemRequest(objectId = offer.id, objectType = offer.objectType)
    },
)

@JvmName("discountDetailsToValidateDiscountsRequest")
internal fun List<AppliedDiscount>.toValidateDiscountsRequest(
    orderId: String,
    orderValue: String,
): ValidateDiscountsRequest = ValidateDiscountsRequest(
    orderId = orderId,
    orderValue = orderValue,
    items = map { detail ->
        DiscountItemRequest(objectId = detail.objectId, objectType = detail.objectType)
    },
)

internal fun List<AppliedDiscount>.toCreateRedemptionRequest(
    orderId: String,
    orderValue: String,
): CreateRedemptionRequest = CreateRedemptionRequest(
    orderId = orderId,
    orderValue = orderValue,
    items = map { detail ->
        RedemptionItemRequest(
            objectId = detail.objectId,
            objectType = detail.objectType,
            expectedDiscount = detail.calculatedDiscount,
        )
    },
)

// ─── Map domain result → AppliedDiscount (model public) ──────────────────────

/**
 * Dựng [AppliedDiscount] cho offer [objectId]/[objectType] từ kết quả validate, đi qua đúng hai helper
 * domain [ValidateDiscountsResult.isValidFor] & [ValidateDiscountsResult.discountFor] — **đối xứng iOS**
 * (iOS gọi thẳng hai helper này per-offer). Nhờ vậy cả hai nền tảng diễn giải valid/discount cùng một rule,
 * lặp theo **offer đã chọn** thay vì map mù `response.items`.
 */
internal fun ValidateDiscountsResult.appliedDiscountFor(objectId: String, objectType: String): AppliedDiscount =
    AppliedDiscount(
        objectId = objectId,
        objectType = objectType,
        valid = isValidFor(objectId),
        calculatedDiscount = discountFor(objectId),
        eligibilityStatus = itemFor(objectId)?.eligibilityStatus.orEmpty(),
    )

internal fun AvailableService.toServiceSelectorUiItem(): ServiceSelectorUiItem = ServiceSelectorUiItem(
    serviceCode = serviceCode,
    serviceName = serviceName,
    iconUrl = iconUrl,
)

// ─── Map EligibleOffer → model UI ────────────────────────────────────────────

/**
 * [EligibleOffer] (luồng checkout, API `findEligible`) → model UI dùng chung với luồng
 * "Ưu đãi của tôi".
 *
 * Khác `VoucherItem.toMyVoucherListItem()`:
 *  - [MyVoucherListItem.voucherId] mang [EligibleOffer.id] (= `voucherId` nếu khách đã sở hữu,
 *    ngược lại `campaignId`). Đây cũng là `objectId` gửi lên `validateDiscounts` / `createRedemption`.
 *  - Không có `merchantName` / `logo` / `applicableProducts`: `findEligible` không trả về.
 *  - [MyVoucherListItem.isAutoApplied] luôn `false`.
 *
 * TODO(auto-apply): `findEligible` chưa trả `isAutoApplied` (không có ở `EligibleOfferDto` lẫn các
 * DTO lồng bên trong). Voucher tự-áp-dụng vì thế **không chạy** ở luồng checkout. Khi backend bổ
 * sung field, thêm vào `EligibleOfferDto` + [EligibleOffer], gán ở đây, rồi bật lại nhánh
 * auto-apply trong `EndowViewModel`.
 */
internal fun EligibleOffer.toMyVoucherListItem(): MyVoucherListItem = MyVoucherListItem(
    voucherId = id,
    campaignId = campaignId.orEmpty(),
    // Tên hiển thị ưu tiên đối tác/merchant (partnerName, v1.6); số tiền giảm ở dòng nội dung.
    merchantName = partnerName ?: campaignName.orEmpty(),
    title = formatEstimatedDiscount(estimatedDiscount).orEmpty(),
    description = campaignName.orEmpty(),
    // logoUrl (v1.6) — trước đây findEligible không trả, card offer để trống logo.
    logo = logoUrl.orEmpty(),
    expirationDate = expireDate.orEmpty(),
    // Lõi không dựng sẵn câu tiếng Việt; lý do lấy từ rule đầu tiên không khớp. Rỗng → layout tự
    // hiện nhãn mặc định của nó.
    displayStatusLabel = if (usable) "" else unmatchedRules.firstOrNull().orEmpty(),
    // `usable=false` nghĩa là server trả `displayMode=DISABLED`: hiện mờ, không cho chọn.
    // INELIGIBLE là trạng thái duy nhất có `displayState()` = INELIGIBLE mà không mang nghĩa
    // "đã dùng"/"hết hạn" — đúng ngữ nghĩa ở đây.
    status = if (usable) VoucherStatus.AVAILABLE else VoucherStatus.INELIGIBLE,
    objectType = objectType,
    isAutoApplied = false,
    applicableProducts = emptyList(),
)

/**
 * `"50000"` → `"Giảm giá 50.000đ"`; `null`/`"0"`/chuỗi không có số → `null`.
 *
 * Chuỗi lặp lại `R.string.prm_discount_amount_format` vì ViewModel không giữ `Context`;
 * sửa một chỗ thì sửa cả hai.
 */
private fun formatEstimatedDiscount(raw: String?): String? {
    val digits = raw?.filter { it.isDigit() }.orEmpty()
    val value = digits.toLongOrNull() ?: return null
    if (value <= 0) return null
    return "Giảm giá ${value.groupedByThousands()}đ"
}

private fun Long.groupedByThousands(): String =
    toString().reversed().chunked(3).joinToString(".").reversed()

// Cảnh báo "sắp hết hạn" (expireWarningDate) nay do store (promotionLogic) tính một lần cho cả 2 nền
// tảng — `MyPromotionVoucher.expiringInDays` / `ChooseOffer.expiringInDays`. Không còn tính lại ở đây.
