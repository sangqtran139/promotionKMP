package com.ttcn.promotionsdk.domain.model.stackablediscount

public data class ValidateDiscountsResult(
    val overallValid: Boolean,
    val totalDiscountAmount: String,
    val finalAmount: String,
    val items: List<DiscountItemResult>,
    /**
     * Lý do cấp-đơn server từ chối (`businessRuleViolations[].message`), ví dụ `"Voucher not found"`.
     *
     * Trước đây DTO parse field này rồi **bỏ đi** — grep cả `commonMain` không nơi nào đọc. Hậu quả:
     * validate trả `valid=false` mà không ai biết vì sao, native không có gì để hiện, còn dev thì
     * phải mở log mạng mới thấy. Giữ lại ở đây để cả hai nền tảng dùng chung.
     */
    val businessRuleViolations: List<String> = emptyList(),
) {
    val validItems: List<DiscountItemResult> get() = items.filter { it.valid }
    val invalidItems: List<DiscountItemResult> get() = items.filter { !it.valid }

    // ─── Diễn giải kết quả cho MỘT offer (áp/auto-apply đơn lẻ) — dùng chung Android & iOS ───

    /** Dòng kết quả ứng với [objectId]; null nếu server không trả dòng nào cho offer này. */
    public fun itemFor(objectId: String): DiscountItemResult? = items.firstOrNull { it.objectId == objectId }

    /**
     * Offer có được áp không: chỉ chặn khi server nói rõ `valid == false`. Không có dòng riêng
     * ([itemFor] null) → không chặn (khớp hành vi iOS/Android hiện tại).
     */
    public fun isValidFor(objectId: String): Boolean = itemFor(objectId)?.valid != false

    /**
     * Số tiền giảm áp cho [objectId]: lấy `calculatedDiscount` của dòng riêng, thiếu thì fallback
     * `totalDiscountAmount` của cả kết quả (chuỗi số thô — tầng hiển thị tự format).
     */
    public fun discountFor(objectId: String): String = itemFor(objectId)?.calculatedDiscount ?: totalDiscountAmount

    /**
     * Lý do đọc được để hiện/log cho [objectId]: ưu tiên thông điệp của chính dòng đó
     * (`validationMessages`), không có thì lùi về lý do cấp đơn ([businessRuleViolations]).
     */
    public fun reasonFor(objectId: String): List<String> =
        itemFor(objectId)?.validationMessages?.takeIf { it.isNotEmpty() } ?: businessRuleViolations
}

public data class DiscountItemResult(
    val objectId: String,
    val objectType: String,
    val valid: Boolean,
    val calculatedDiscount: String,
    val eligibilityStatus: String,
    val tags: List<String> = emptyList(),
    /** Lý do cấp-dòng server trả (`discountDetails[].validationMessages`). */
    val validationMessages: List<String> = emptyList(),
)
