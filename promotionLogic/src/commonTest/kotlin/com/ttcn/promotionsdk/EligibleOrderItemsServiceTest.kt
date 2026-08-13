package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.eligibleOrderItems
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOrderItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `serviceCode` → `orderInfo.items[].productId` — đường DUY NHẤT mà spec findEligible v19 nhận chiều
 * dịch vụ (body schema không có field `serviceCode` ở bất kỳ cấp nào).
 *
 * Trước khi có `eligibleOrderItems()`, `getService()` không hề có call-site production: host truyền
 * `serviceCode` qua `updateContext` mà request findEligible không bao giờ nhận được.
 */
class EligibleOrderItemsServiceTest {

    private class Ctx(
        private val service: String? = null,
        private val items: List<EligibleOrderItem> = emptyList(),
    ) : PromotionRequestContextProvider {
        override fun getService(): String? = service
        override fun getOrderItems(): List<EligibleOrderItem> = items
    }

    private fun item(sku: String, productId: String? = null) =
        EligibleOrderItem(skuId = sku, quantity = 1, unitPrice = "1000", productId = productId)

    @Test
    fun fillsProductIdFromServiceCode() {
        val out = Ctx(service = "TKBAOVIET", items = listOf(item("SKU-1"), item("SKU-2")))
            .eligibleOrderItems()
        assertEquals(listOf("TKBAOVIET", "TKBAOVIET"), out.map { it.productId })
        assertEquals(listOf("SKU-1", "SKU-2"), out.map { it.skuId }, "không đụng field khác")
    }

    @Test
    fun keepsProductIdAlreadySetByHost() {
        // Host khai rõ dòng hàng thì host đúng hơn context cấp đơn.
        val out = Ctx(service = "TKBAOVIET", items = listOf(item("SKU-1", productId = "SAVINGS_DEPOSIT")))
            .eligibleOrderItems()
        assertEquals("SAVINGS_DEPOSIT", out.single().productId)
    }

    @Test
    fun leavesItemsUntouchedWhenNoServiceCode() {
        assertNull(Ctx(items = listOf(item("SKU-1"))).eligibleOrderItems().single().productId)
    }

    @Test
    fun blankServiceCodeCountsAsAbsent() {
        // `updateContext(serviceCode = "")` không được biến thành `productId = ""` — server sẽ lọc
        // theo một product rỗng và trả về danh sách trống.
        assertNull(Ctx(service = "   ", items = listOf(item("SKU-1"))).eligibleOrderItems().single().productId)
    }

    @Test
    fun emptyItemsStayEmpty() {
        // Không tự dựng item từ `serviceCode`: spec bắt `skuSourceId` là **required**, mà context
        // cấp đơn không có SKU nào để điền. Items rỗng → server chỉ chạy rule cấp đơn (spec §4.3).
        assertEquals(emptyList(), Ctx(service = "TKBAOVIET").eligibleOrderItems())
    }
}
