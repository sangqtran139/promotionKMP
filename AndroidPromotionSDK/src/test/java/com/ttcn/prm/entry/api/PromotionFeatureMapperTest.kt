package com.ttcn.prm.entry.api

import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlags as CoreFeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ranh giới lõi ↔ DTO public của feature flag. Kotlin thuần, không đụng Android → chạy được bằng
 * unit test JVM, không cần Robolectric (giống `PromotionThemeJsonTest`).
 */
class PromotionFeatureMapperTest {

    @Test
    fun `moi PromotionFeature anh xa dung hang so cua loi`() {
        assertEquals(PromotionFeatureFlag.ENABLE_ALL, PromotionFeature.ALL.flagName())
        assertEquals(PromotionFeatureFlag.VOUCHER_LIST, PromotionFeature.VOUCHER_LIST.flagName())
        assertEquals(PromotionFeatureFlag.VOUCHER_DETAIL, PromotionFeature.VOUCHER_DETAIL.flagName())
        assertEquals(PromotionFeatureFlag.VOUCHER_SELECTION, PromotionFeature.VOUCHER_SELECTION.flagName())
        assertEquals(PromotionFeatureFlag.VOUCHER_APPLY, PromotionFeature.VOUCHER_APPLY.flagName())
        assertEquals(PromotionFeatureFlag.VOUCHER_REDEEM, PromotionFeature.VOUCHER_REDEEM.flagName())
    }

    @Test
    fun `snapshot giu nguyen tung co khi cong tac tong dang bat`() {
        val snapshot = CoreFeatureFlags(
            enableAll = true,
            voucherApply = true,
            voucherRedeem = false,
            voucherSelection = true,
            voucherDetail = false,
            voucherList = true,
        ).toSnapshot()

        assertTrue(snapshot.all)
        assertTrue(snapshot.voucherList)
        assertFalse(snapshot.voucherDetail)
        assertTrue(snapshot.voucherSelection)
        assertTrue(snapshot.voucherApply)
        assertFalse(snapshot.voucherRedeem)
    }

    /**
     * Đây là lý do `toSnapshot()` đi qua `isEnabled(...)` chứ không đọc thẳng field: field thô vẫn
     * `true` khi công tắc tổng tắt, host đọc phải sẽ hiện nhầm entry point.
     */
    @Test
    fun `cong tac tong tat thi moi co con deu tat`() {
        val snapshot = CoreFeatureFlags(
            enableAll = false,
            voucherApply = true,
            voucherRedeem = true,
            voucherSelection = true,
            voucherDetail = true,
            voucherList = true,
        ).toSnapshot()

        assertFalse(snapshot.all)
        assertFalse(snapshot.voucherList)
        assertFalse(snapshot.voucherDetail)
        assertFalse(snapshot.voucherSelection)
        assertFalse(snapshot.voucherApply)
        assertFalse(snapshot.voucherRedeem)
    }

    @Test
    fun `isEnabled tren snapshot khop tung field`() {
        val snapshot = PromotionFeatureFlagsSnapshot(
            all = true,
            voucherList = true,
            voucherDetail = false,
            voucherSelection = true,
            voucherApply = false,
            voucherRedeem = true,
        )

        assertTrue(snapshot.isEnabled(PromotionFeature.ALL))
        assertTrue(snapshot.isEnabled(PromotionFeature.VOUCHER_LIST))
        assertFalse(snapshot.isEnabled(PromotionFeature.VOUCHER_DETAIL))
        assertTrue(snapshot.isEnabled(PromotionFeature.VOUCHER_SELECTION))
        assertFalse(snapshot.isEnabled(PromotionFeature.VOUCHER_APPLY))
        assertTrue(snapshot.isEnabled(PromotionFeature.VOUCHER_REDEEM))
    }

    @Test
    fun `mac dinh fail-open bat het`() {
        val default = PromotionFeatureFlagsSnapshot.AllEnabled
        PromotionFeature.entries.forEach { assertTrue(default.isEnabled(it)) }
    }
}
