package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.domain.model.voucher.SearchCustomerVouchersResult
import com.ttcn.promotionsdk.domain.model.voucher.VoucherTabItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Khoá quy tắc chọn tab active của màn "Ưu đãi của tôi".
 *
 * Trước khi gộp, rule này nằm inline trong `MyPromotionViewModel` (Android) còn iOS bỏ qua hoàn toàn
 * (luôn mặc định "all"). Đưa xuống [SearchCustomerVouchersResult.resolveActiveTab] để hai nền tảng
 * đọc chung; test này giữ cho thứ tự ưu tiên không lệch lại.
 */
class ResolveActiveTabTest {

    private fun result(
        selectedTab: String? = null,
        defaultTab: String? = null,
        tabs: List<VoucherTabItem> = emptyList(),
    ) = SearchCustomerVouchersResult(selectedTab = selectedTab, defaultTab = defaultTab, tabs = tabs)

    private fun tab(code: String, order: Int?) = VoucherTabItem(code = code, label = code, order = order)

    @Test
    fun selectedTab_winsOverEverything() {
        val r = result(selectedTab = "used", defaultTab = "all", tabs = listOf(tab("expiring", 0)))
        assertEquals("used", r.resolveActiveTab(requestedTab = "active"))
    }

    @Test
    fun defaultTab_whenNoSelected() {
        val r = result(defaultTab = "all", tabs = listOf(tab("expiring", 0)))
        assertEquals("all", r.resolveActiveTab(requestedTab = "active"))
    }

    @Test
    fun requestedTab_whenNoServerTab() {
        val r = result(tabs = listOf(tab("expiring", 0)))
        assertEquals("active", r.resolveActiveTab(requestedTab = "active"))
    }

    @Test
    fun firstTabByOrder_asLastResort_ignoringRawListOrder() {
        // List thô KHÔNG theo order (mapper không sort); phải chọn tab order nhỏ nhất.
        val r = result(tabs = listOf(tab("used", 2), tab("all", 0), tab("expiring", 1)))
        assertEquals("all", r.resolveActiveTab())
    }

    @Test
    fun nullOrder_treatedAsLowestPriority() {
        val r = result(tabs = listOf(tab("noorder", null), tab("all", 0)))
        assertEquals("all", r.resolveActiveTab())
    }

    @Test
    fun allNull_andNoTabs_returnsNull() {
        assertNull(result().resolveActiveTab())
    }
}
