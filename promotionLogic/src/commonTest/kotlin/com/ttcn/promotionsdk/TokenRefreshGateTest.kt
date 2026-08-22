package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.data.remote.TokenRefreshGate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [TokenRefreshGate] đứng giữa lõi và **code của host** — thứ SDK không kiểm soát được. Test ở đây
 * chủ yếu nhắm vào các cách host có thể cư xử sai, chứ không phải đường chạy đúng
 * (`TokenRefreshRetryTest` lo phần đó, xuyên qua cả HTTP).
 *
 * `runTest` chạy thời gian ảo nên nhánh hết-giờ-chờ không tốn 15 giây thật.
 */
class TokenRefreshGateTest {

    /** Host giả, cư xử theo [behaviour] khi bị hỏi. */
    private class Host(private val behaviour: ((Boolean) -> Unit) -> Unit) : PromotionRequestContextProvider {
        var askedCount = 0
            private set

        override fun refreshAccessToken(onResult: (Boolean) -> Unit) {
            askedCount++
            behaviour(onResult)
        }
    }

    // ─── Đường chạy đúng ──────────────────────────────────────────────────────

    @Test
    fun hostBaoThanhCong_thiChoThuLai_vaTangGeneration() = runTest {
        val host = Host { it(true) }
        val gate = TokenRefreshGate(host)

        assertEquals(0, gate.generation)
        assertTrue(gate.refresh(seenGeneration = 0))
        assertEquals(1, gate.generation)
        assertEquals(1, host.askedCount)
    }

    @Test
    fun hostBaoChiu_thiKhongThuLai_vaGiuNguyenGeneration() = runTest {
        val host = Host { it(false) }
        val gate = TokenRefreshGate(host)

        assertFalse(gate.refresh(seenGeneration = 0))
        assertEquals(0, gate.generation)
        assertEquals(1, host.askedCount)
    }

    @Test
    fun mac_dinh_cua_provider_la_chiu_ngay() = runTest {
        // Host không cài đặt `refreshAccessToken` → default `onResult(false)` của interface.
        val gate = TokenRefreshGate(object : PromotionRequestContextProvider {})

        assertFalse(gate.refresh(seenGeneration = 0))
        assertEquals(0, gate.generation)
    }

    // ─── Single-flight ────────────────────────────────────────────────────────

    @Test
    fun generationDaDoi_thiKhongHoiHostNua() = runTest {
        // Request này gửi đi với token của generation 0, nhưng lúc nó hỏng thì ai đó đã refresh xong
        // (generation = 1). Không có gì để làm thêm — cứ thử lại.
        val host = Host { it(true) }
        val gate = TokenRefreshGate(host)
        gate.refresh(seenGeneration = 0)          // generation: 0 → 1
        assertEquals(1, host.askedCount)

        assertTrue(gate.refresh(seenGeneration = 0))
        assertEquals(1, host.askedCount, "không được hỏi host lần thứ hai")
    }

    @Test
    fun nhieuRequestCungHong_chiHoiHostMotLan() = runTest {
        val host = Host { it(true) }
        val gate = TokenRefreshGate(host)

        // Bốn request cùng chụp generation 0 rồi cùng 401 — đúng cảnh mở một màn.
        val results = List(4) { async { gate.refresh(seenGeneration = 0) } }.awaitAll()

        assertTrue(results.all { it }, "cả bốn đều phải được phép thử lại")
        assertEquals(1, host.askedCount)
        assertEquals(1, gate.generation)
    }

    // ─── Host cư xử sai ───────────────────────────────────────────────────────

    @Test
    fun hostKhongBaoGioGoiLai_thiBoCuocChuKhongTreo() = runTest {
        // Không có chốt hết giờ thì `Mutex` bị giữ vĩnh viễn → MỌI API của SDK chết theo.
        val host = Host { /* nuốt luôn callback */ }
        val gate = TokenRefreshGate(host)

        assertFalse(gate.refresh(seenGeneration = 0))
        assertEquals(0, gate.generation)

        // Và cổng vẫn dùng được cho lượt sau — khoá đã được nhả.
        val gate2 = TokenRefreshGate(Host { it(true) })
        assertTrue(gate2.refresh(seenGeneration = 0))
    }

    @Test
    fun hostGoiCallbackHaiLan_thiKhongNo() = runTest {
        // `Continuation.resume` lần hai ném IllegalStateException từ một thread lạ.
        val host = Host { onResult ->
            onResult(true)
            onResult(true)
            onResult(false)
        }
        val gate = TokenRefreshGate(host)

        assertTrue(gate.refresh(seenGeneration = 0), "chỉ lần gọi ĐẦU được tính")
        assertEquals(1, gate.generation)
    }

    @Test
    fun sauKhiBoCuoc_lanRefreshKeTiepVanChayDuoc() = runTest {
        var answerNextTime = false
        val host = Host { onResult -> if (answerNextTime) onResult(true) }
        val gate = TokenRefreshGate(host)

        assertFalse(gate.refresh(seenGeneration = 0))   // hết giờ

        answerNextTime = true
        assertTrue(gate.refresh(seenGeneration = 0))
        assertEquals(1, gate.generation)
        assertEquals(2, host.askedCount)
    }
}
