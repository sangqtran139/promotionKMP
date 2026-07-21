package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemResult
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Khoá cách diễn giải kết quả validate cho MỘT offer (áp/auto-apply đơn lẻ).
 *
 * Trước khi gộp, rule "lấy dòng theo objectId → valid? → discount = calculatedDiscount ?? total"
 * bị lặp 2 lần trong `PromotionSDKImpl` (iOS). Đưa xuống [ValidateDiscountsResult] để dùng chung.
 */
class ValidateDiscountsOutcomeTest {

    private fun item(id: String, valid: Boolean, discount: String) =
        DiscountItemResult(objectId = id, objectType = "CAMPAIGN", valid = valid, calculatedDiscount = discount, eligibilityStatus = "")

    private fun result(vararg items: DiscountItemResult) =
        ValidateDiscountsResult(overallValid = true, totalDiscountAmount = "9000", finalAmount = "0", items = items.toList())

    @Test
    fun itemFor_findsMatchingObjectId() {
        val r = result(item("a", true, "1000"), item("b", false, "2000"))
        assertEquals("b", r.itemFor("b")?.objectId)
        assertNull(r.itemFor("zzz"))
    }

    @Test
    fun isValidFor_blocksOnlyWhenExplicitlyFalse() {
        val r = result(item("a", true, "1000"), item("b", false, "2000"))
        assertTrue(r.isValidFor("a"))
        assertFalse(r.isValidFor("b"))
    }

    @Test
    fun isValidFor_missingItem_doesNotBlock() {
        // Không có dòng riêng → không chặn (khớp iOS/Android hiện tại).
        assertTrue(result(item("a", true, "1000")).isValidFor("missing"))
    }

    @Test
    fun discountFor_usesItemDiscount_whenPresent() {
        assertEquals("1000", result(item("a", true, "1000")).discountFor("a"))
    }

    @Test
    fun discountFor_fallsBackToTotal_whenNoItem() {
        // totalDiscountAmount = "9000".
        assertEquals("9000", result(item("a", true, "1000")).discountFor("missing"))
    }
}
