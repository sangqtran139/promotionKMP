package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.common.currentEpochMillis
import com.ttcn.promotionsdk.common.daysUntil
import com.ttcn.promotionsdk.common.isoDateToEpochMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `PromotionClock.kt` từng bị `reports.filters.excludes` của Kover che hoàn toàn vì nó **có** một hàm
 * cầu nền tảng — nhưng ba hàm còn lại là toán ngày tháng thật (`daysFromCivil` của Howard Hinnant),
 * và chúng quyết định chuỗi "HSD còn X ngày" trên mọi màn. Test này ra đời cùng lúc với việc gỡ
 * exclude đó.
 *
 * `daysUntil` nhận `now` tường minh nên **không cần mock đồng hồ** — kết quả tất định trên cả hai nền tảng.
 */
class PromotionClockTest {

    private val msPerDay = 86_400_000L

    // ── isoDateToEpochMillis ────────────────────────────────────────────────

    @Test
    fun epochOrigin_isZero() {
        assertEquals(0L, isoDateToEpochMillis("1970-01-01"))
    }

    /** Mốc Y2K quen thuộc — sai một ngày ở đây là sai toàn bộ thuật toán. */
    @Test
    fun y2k_matchesKnownEpoch() {
        assertEquals(946_684_800_000L, isoDateToEpochMillis("2000-01-01"))
    }

    /** Ngày nhuận: 29/02/2024 chỉ đúng nếu quy tắc leap-year được áp đủ cả ba tầng (4/100/400). */
    @Test
    fun leapDay_2024_isCorrect() {
        assertEquals(1_709_164_800_000L, isoDateToEpochMillis("2024-02-29"))
    }

    /** Phần giờ bị cắt bằng `take(10)` — cùng ngày phải ra cùng giá trị. */
    @Test
    fun timePart_isIgnored() {
        assertEquals(
            isoDateToEpochMillis("2024-02-29"),
            isoDateToEpochMillis("2024-02-29T13:45:07")
        )
    }

    /** Trước epoch → âm. Nhánh `month <= 2` (lùi năm) đi qua đây. */
    @Test
    fun beforeEpoch_isNegative() {
        assertEquals(-msPerDay, isoDateToEpochMillis("1969-12-31"))
    }

    /** Nhánh `y < 0` của `daysFromCivil` chỉ với tới được khi năm 0, tháng ≤ 2. */
    @Test
    fun yearZero_januaryDoesNotCrash() {
        val result = isoDateToEpochMillis("0000-01-15")
        assertTrue(result != null && result < 0)
    }

    @Test
    fun malformedInput_returnsNull() {
        assertNull(isoDateToEpochMillis(null))
        assertNull(isoDateToEpochMillis(""))
        assertNull(isoDateToEpochMillis("   "))
        assertNull(isoDateToEpochMillis("2024-02"))        // thiếu đoạn
        assertNull(isoDateToEpochMillis("20240229"))       // không có dấu -
        assertNull(isoDateToEpochMillis("xxxx-01-01"))     // năm không phải số
        assertNull(isoDateToEpochMillis("2024-ab-01"))     // tháng không phải số
        assertNull(isoDateToEpochMillis("2024-01-cd"))     // ngày không phải số
        assertNull(isoDateToEpochMillis("2024-13-01"))     // tháng ngoài 1..12
        assertNull(isoDateToEpochMillis("2024-00-01"))
        assertNull(isoDateToEpochMillis("2024-01-32"))     // ngày ngoài 1..31
        assertNull(isoDateToEpochMillis("2024-01-00"))
    }

    // ── daysUntil ───────────────────────────────────────────────────────────

    @Test
    fun daysUntil_countsWholeDays() {
        assertEquals(2, daysUntil("1970-01-03", now = 0L))
    }

    /** Làm tròn LÊN: còn 1ms cũng phải hiện "1 ngày", không được thành 0. */
    @Test
    fun daysUntil_roundsUp_soAlmostExpiredStillShowsOne() {
        assertEquals(1, daysUntil("1970-01-02", now = msPerDay - 1))
    }

    /** Đúng thời khắc hết hạn → 0, và `ExpiryWarning` vẫn tính là "sắp hết hạn" vì rule là `0..warn`. */
    @Test
    fun daysUntil_atExactExpiry_isZero() {
        assertEquals(0, daysUntil("1970-01-02", now = msPerDay))
    }

    /** Đã quá hạn → âm, để `it in 0..warn` của ExpiryWarning loại ra. */
    @Test
    fun daysUntil_afterExpiry_isNegative() {
        val result = daysUntil("1970-01-01", now = 2 * msPerDay)
        assertTrue(result != null && result < 0, "quá hạn phải cho số âm, nhận $result")
    }

    @Test
    fun daysUntil_unparseableDate_isNull() {
        assertNull(daysUntil(null, now = 0L))
        assertNull(daysUntil("khong-phai-ngay", now = 0L))
    }

    // ── currentEpochMillis ──────────────────────────────────────────────────

    /**
     * Sau khi bỏ `expect/actual`, hàm này chạy bằng `kotlin.time.Clock` của stdlib. Chỉ cần khẳng
     * định nó trả thời gian thật (không phải 0, không phải hằng số) — mốc so sánh là 2024-01-01.
     */
    @Test
    fun currentEpochMillis_returnsRealTime() {
        val now = currentEpochMillis()
        assertTrue(now > 1_704_067_200_000L, "đồng hồ trả $now, nhỏ hơn mốc 2024-01-01")
    }
}
