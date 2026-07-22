package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleSection
import com.ttcn.promotionsdk.core.domain.model.eligible.FindEligibleCampaignsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Khoá quy tắc phân trang ĐỘC LẬP 2 nhóm của màn "Chọn ưu đãi".
 *
 * Trước khi gộp, rule "chỉ nhóm được yêu cầu mới tiến trang, nhóm kia giữ nguyên" bị lặp ở
 * `ChoosePromotionViewModel` (Android, trong `buildRequest`) và rải khắp 3 call-site iOS. Đưa xuống
 * [FindEligibleCampaignsRequest.forSectionPage] để hai nền tảng dùng chung; test giữ cho không lệch.
 */
class ForSectionPageTest {

    private val base = FindEligibleCampaignsRequest(orderId = "o-1", orderValue = "100000")

    @Test
    fun sectionNull_loadsBothFromNextPage() {
        val r = base.forSectionPage(section = null, nextPage = 0, currentMyPage = 3, currentOtherPage = 5)
        assertEquals(0, r.myPage)
        assertEquals(0, r.otherPage)
        assertNull(r.section)
    }

    @Test
    fun myOffers_advancesMyOnly_keepsOther() {
        val r = base.forSectionPage(section = EligibleSection.MY_OFFERS, nextPage = 4, currentMyPage = 3, currentOtherPage = 5)
        assertEquals(4, r.myPage)
        assertEquals(5, r.otherPage)
        assertEquals(EligibleSection.MY_OFFERS, r.section)
    }

    @Test
    fun otherOffers_advancesOtherOnly_keepsMy() {
        val r = base.forSectionPage(section = EligibleSection.OTHER_OFFERS, nextPage = 6, currentMyPage = 3, currentOtherPage = 5)
        assertEquals(3, r.myPage)
        assertEquals(6, r.otherPage)
        assertEquals(EligibleSection.OTHER_OFFERS, r.section)
    }

    @Test
    fun preservesOtherFields() {
        val src = base.copy(keyword = "grab", mySize = 20, otherSize = 15)
        val r = src.forSectionPage(section = EligibleSection.MY_OFFERS, nextPage = 1, currentMyPage = 0, currentOtherPage = 2)
        assertEquals("grab", r.keyword)
        assertEquals(20, r.mySize)
        assertEquals(15, r.otherSize)
        assertEquals("o-1", r.orderId)
    }
}
