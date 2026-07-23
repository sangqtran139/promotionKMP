package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionSessionResponse
import com.ttcn.promotionsdk.core.data.dto.redemption.RedemptionValidationErrorResponse
import com.ttcn.promotionsdk.core.data.dto.redemption.SessionPreview
import com.ttcn.promotionsdk.core.data.dto.redemption.toCreateRedemptionResult
import com.ttcn.promotionsdk.core.data.dto.redemption.toRedemptionSessionRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountDetail
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsResponse
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackingValidationResult
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.toStackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.toValidateDiscountsResult
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Khoá hợp đồng mapper DTO ⇄ domain của hai luồng tiền: validate stackable discount và
 * create redemption. Đây là chỗ dễ lệch âm thầm khi BFF đổi field.
 */
class DiscountMapperTest {

    // ─── ValidateDiscountsRequest → StackableDiscountsRequest ─────────────────

    @Test
    fun validateRequest_mapsOrderAndAssignsPriorityFromOrder() {
        val req = ValidateDiscountsRequest(
            orderId = "ORD-1", orderValue = "10000",
            items = listOf(
                DiscountItemRequest("a", "CAMPAIGN"),
                DiscountItemRequest("b", "VOUCHER"),
            ),
        ).toStackableDiscountsRequest()

        assertEquals("ORD-1", req.orderInfo.orderId)
        assertEquals("10000", req.orderInfo.orderValue)
        // priority = vị trí trong list + 1 (server dùng để xếp thứ tự áp).
        assertEquals(listOf(1, 2), req.discountRequests.map { it.priority })
        assertEquals(listOf("a", "b"), req.discountRequests.map { it.objectId })
        assertEquals(listOf("CAMPAIGN", "VOUCHER"), req.discountRequests.map { it.objectType })
    }

    @Test
    fun validateRequest_idempotencyKeyIsFreshEachCall() {
        val src = ValidateDiscountsRequest("ORD-1", "1", listOf(DiscountItemRequest("a")))
        assertNotEquals(src.toStackableDiscountsRequest().idempotencyKey, src.toStackableDiscountsRequest().idempotencyKey)
    }

    @Test
    fun validateRequest_emptyItems() {
        val req = ValidateDiscountsRequest("ORD-1", "1", emptyList()).toStackableDiscountsRequest()
        assertTrue(req.discountRequests.isEmpty())
    }

    // ─── StackableDiscountsResponse → ValidateDiscountsResult ─────────────────

    private fun response(vararg details: DiscountDetail, valid: Boolean = true) =
        StackableDiscountsResponse(
            validationResult = StackingValidationResult(
                overallValid = valid, totalDiscountAmount = "1500", finalAmount = "8500",
            ),
            discountDetails = details.toList(),
        )

    @Test
    fun validateResponse_mapsTotalsAndItems() {
        val r = response(
            DiscountDetail("a", "CAMPAIGN", true, "1000", "ELIGIBLE"),
            DiscountDetail("b", "CAMPAIGN", false, "0", "NOT_ELIGIBLE"),
        ).toValidateDiscountsResult()

        assertTrue(r.overallValid)
        assertEquals("1500", r.totalDiscountAmount)
        assertEquals("8500", r.finalAmount)
        assertEquals(2, r.items.size)
        assertEquals(listOf("a"), r.validItems.map { it.objectId })
        assertEquals(listOf("b"), r.invalidItems.map { it.objectId })
    }

    @Test
    fun validateResponse_perOfferHelpers() {
        val r = response(DiscountDetail("a", "CAMPAIGN", true, "1000", "ELIGIBLE")).toValidateDiscountsResult()

        assertTrue(r.isValidFor("a"))
        assertEquals("1000", r.discountFor("a"))
        assertEquals("ELIGIBLE", r.itemFor("a")?.eligibilityStatus)
    }

    @Test
    fun validateResponse_unknownOffer_isOptimisticallyValidAndFallsBackToTotal() {
        // Khoá 2 quy ước DỄ HIỂU NHẦM (đang được cả Android lẫn iOS dựa vào):
        //  - isValidFor: server KHÔNG trả dòng nào cho offer ⇒ coi là hợp lệ (`?.valid != false`),
        //    tức chỉ đánh trượt khi server nói rõ valid=false.
        //  - discountFor: thiếu dòng riêng ⇒ lùi về totalDiscountAmount của cả kết quả, KHÔNG phải "0".
        val r = response(DiscountDetail("a", "CAMPAIGN", true, "1000", "ELIGIBLE")).toValidateDiscountsResult()

        assertNull(r.itemFor("khong-ton-tai"))
        assertTrue(r.isValidFor("khong-ton-tai"))
        assertEquals("1500", r.discountFor("khong-ton-tai"))
    }

    @Test
    fun validateResponse_explicitInvalid_isTheOnlyWayToFail() {
        val r = response(DiscountDetail("b", "CAMPAIGN", false, "0", "NOT_ELIGIBLE")).toValidateDiscountsResult()
        assertTrue(!r.isValidFor("b"))
    }

    @Test
    fun validateResponse_emptyDetails() {
        val r = response().toValidateDiscountsResult()
        assertTrue(r.items.isEmpty())
        assertTrue(r.validItems.isEmpty())
        assertTrue(r.invalidItems.isEmpty())
    }

    // ─── CreateRedemptionRequest → RedemptionSessionRequest ───────────────────

    @Test
    fun redemptionRequest_mapsItemsWithPriorityAndExpectedDiscount() {
        val req = CreateRedemptionRequest(
            orderId = "ORD-9", orderValue = "20000",
            items = listOf(
                RedemptionItemRequest("a", "CAMPAIGN", expectedDiscount = "1000"),
                RedemptionItemRequest("b", "VOUCHER"),
            ),
        ).toRedemptionSessionRequest()

        assertEquals("ORD-9", req.orderInfo.orderId)
        assertEquals(listOf(1, 2), req.selectedRedeemables.map { it.priority })
        // expectedDiscount null → "" (server không nhận null).
        assertEquals(listOf("1000", ""), req.selectedRedeemables.map { it.expectedDiscount })
    }

    // ─── RedemptionSessionResponse → CreateRedemptionResult ───────────────────

    @Test
    fun redemptionResponse_mapsPreviewAndErrors() {
        val r = RedemptionSessionResponse(
            sessionId = "S-1",
            preview = SessionPreview(orderId = "ORD-9", originalAmount = "20000", totalDiscount = "1000", finalAmount = "19000"),
            validationErrors = listOf(RedemptionValidationErrorResponse(code = "E1", message = "loi")),
        ).toCreateRedemptionResult()

        assertEquals("S-1", r.sessionId)
        assertEquals("1000", r.totalDiscount)
        assertEquals("19000", r.finalAmount)
        assertEquals(listOf("E1"), r.validationErrors.map { it.code })
    }

    @Test
    fun redemptionResponse_missingPreview_givesEmptyAmountsNotNull() {
        // preview null xảy ra khi session bị từ chối — UI vẫn phải đọc được chuỗi rỗng.
        val r = RedemptionSessionResponse(sessionId = "S-2", preview = null).toCreateRedemptionResult()

        assertEquals("S-2", r.sessionId)
        assertEquals("", r.totalDiscount)
        assertEquals("", r.finalAmount)
        assertTrue(r.validationErrors.isEmpty())
    }
}
