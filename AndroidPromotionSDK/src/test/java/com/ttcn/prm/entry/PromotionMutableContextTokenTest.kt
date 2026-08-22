package com.ttcn.prm.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [PromotionMutableContext] phải là ống dẫn **trong suốt** tới [PromotionTokenSource] của host:
 * không cache, không fallback, không nhánh.
 *
 * Đó là toàn bộ thiết kế token của SDK. Thêm bất kỳ trạng thái nào ở đây là tái tạo lại đúng con bug
 * gốc — SDK giữ một bản sao token lệch với host.
 */
class PromotionMutableContextTokenTest {

    private fun contextOf(source: PromotionTokenSource) = PromotionMutableContext(
        PromotionSessionConfig(tokenSource = source, baseUrl = "https://api.example.com"),
    )

    @Test
    fun `doc token qua tokenSource`() {
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = "T1"
        })
        assertEquals("T1", ctx.getAccessToken())
    }

    @Test
    fun `moi lan goi deu doc lai, khong cache`() {
        // Chốt quan trọng nhất: host đổi token mà KHÔNG báo SDK thì lượt đọc kế tiếp vẫn phải thấy.
        var token = "cu"
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = token
        })

        assertEquals("cu", ctx.getAccessToken())
        token = "moi"
        assertEquals("moi", ctx.getAccessToken())
    }

    @Test
    fun `tokenSource tra null thi khong bia ra gia tri nao`() {
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken(): String? = null
        })
        // Không fallback: lõi thấy `null` thì bỏ hẳn header Authorization, lỗi hiện ra ngay thay vì
        // gửi một token cũ trong im lặng.
        assertNull(ctx.getAccessToken())
    }

    @Test
    fun `mac dinh refreshToken la chiu ngay`() {
        // Host không cài đặt → 401 hỏng luôn, không chờ. Đúng cho app không có cách lấy token mới.
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = "T1"
        })

        var result: Boolean? = null
        ctx.refreshAccessToken { result = it }
        assertEquals(false, result)
    }

    @Test
    fun `refreshToken duoc uy thang cho host`() {
        var called = false
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = "T1"
            override fun refreshToken(onResult: (Boolean) -> Unit) {
                called = true
                onResult(true)
            }
        })

        var result: Boolean? = null
        ctx.refreshAccessToken { result = it }

        assertTrue(called)
        assertEquals(true, result)
    }

    @Test
    fun `host bao true thi luot doc sau phai thay token moi`() {
        // Hợp đồng của `refreshToken`: ghi vào kho TRƯỚC rồi mới báo `true`. Test này khoá đúng
        // chuỗi sự kiện mà lõi dựa vào để thử lại request hỏng.
        var token = "chet"
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = token
            override fun refreshToken(onResult: (Boolean) -> Unit) {
                token = "song"
                onResult(true)
            }
        })

        assertEquals("chet", ctx.getAccessToken())
        ctx.refreshAccessToken { assertTrue(it) }
        assertEquals("song", ctx.getAccessToken())
    }

    @Test
    fun `refresh hong thi token giu nguyen`() {
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = "chet"
            override fun refreshToken(onResult: (Boolean) -> Unit) = onResult(false)
        })

        var result: Boolean? = null
        ctx.refreshAccessToken { result = it }

        assertFalse(result!!)
        assertEquals("chet", ctx.getAccessToken())
    }

    @Test
    fun `context don hang khong dinh dang gi toi token`() {
        val ctx = contextOf(object : PromotionTokenSource {
            override fun currentToken() = "T1"
        })
        ctx.orderId = "O-1"
        ctx.serviceCode = "TOPUP"

        assertEquals("T1", ctx.getAccessToken())
        assertEquals("O-1", ctx.getOrderId())
        assertEquals("TOPUP", ctx.getService())
    }
}
