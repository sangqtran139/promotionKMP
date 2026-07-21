package com.ttcn.promotionsdk.core.domain.model.stackablediscount

data class ValidateDiscountsResult(
    val overallValid: Boolean,
    val totalDiscountAmount: String,
    val finalAmount: String,
    val items: List<DiscountItemResult>,
) {
    val validItems: List<DiscountItemResult> get() = items.filter { it.valid }
    val invalidItems: List<DiscountItemResult> get() = items.filter { !it.valid }

    // ─── Diễn giải kết quả cho MỘT offer (áp/auto-apply đơn lẻ) — dùng chung Android & iOS ───

    /** Dòng kết quả ứng với [objectId]; null nếu server không trả dòng nào cho offer này. */
    fun itemFor(objectId: String): DiscountItemResult? = items.firstOrNull { it.objectId == objectId }

    /**
     * Offer có được áp không: chỉ chặn khi server nói rõ `valid == false`. Không có dòng riêng
     * ([itemFor] null) → không chặn (khớp hành vi iOS/Android hiện tại).
     */
    fun isValidFor(objectId: String): Boolean = itemFor(objectId)?.valid != false

    /**
     * Số tiền giảm áp cho [objectId]: lấy `calculatedDiscount` của dòng riêng, thiếu thì fallback
     * `totalDiscountAmount` của cả kết quả (chuỗi số thô — tầng hiển thị tự format).
     */
    fun discountFor(objectId: String): String = itemFor(objectId)?.calculatedDiscount ?: totalDiscountAmount
}

data class DiscountItemResult(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
)
