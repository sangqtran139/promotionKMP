package com.ttcn.prm.ui.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat

/**
 * Khoá lại phần chọn ngôn ngữ của SDK — đối ứng `PromotionLocalizationTests` bên iOS.
 *
 * Chỉ phủ [PRMLocale.configure] / [PRMLocale.current]: đó là phần **quyết định** ngôn ngữ, và là
 * phần chạy được trên JVM thuần. [PRMLocale.wrap] cần `Context` thật nên thuộc về instrumented
 * test — repo hiện chưa có tầng đó, xem TEST-1 trong bản rà soát.
 *
 * Vì sao đáng test: trước đây `language` là tham số public **chỉ** đi xuống header của Ktor — một
 * tham số không làm điều mà tên nó nói, và không có gì phát hiện ra.
 */
class PRMLocaleTest {

    /** [PRMLocale] là state tĩnh dùng chung cả process test — trả về mặc định sau mỗi test. */
    @After
    fun tearDown() {
        PRMLocale.configure(PRMLocale.DEFAULT_LANGUAGE)
    }

    @Test
    fun `mac dinh la tieng Viet`() {
        PRMLocale.configure(PRMLocale.DEFAULT_LANGUAGE)

        assertEquals("vi", PRMLocale.current().language)
    }

    @Test
    fun `host truyen ma ngan thi van nhan dung`() {
        PRMLocale.configure("en")

        assertEquals("en", PRMLocale.current().language)
    }

    @Test
    fun `host truyen ma day du thi lay phan ngon ngu`() {
        PRMLocale.configure("en-US")

        assertEquals("en", PRMLocale.current().language)
        assertEquals("US", PRMLocale.current().country)
    }

    /** `null` / rỗng / toàn khoảng trắng đều phải lùi về mặc định, không được ra locale rỗng. */
    @Test
    fun `language rong thi lui ve mac dinh`() {
        PRMLocale.configure("   ")
        assertEquals("vi", PRMLocale.current().language)

        PRMLocale.configure(null)
        assertEquals("vi", PRMLocale.current().language)
    }

    /** Tag rác không được làm `getString` rơi về locale của **máy** — phải lùi về mặc định của SDK. */
    @Test
    fun `tag khong hop le thi lui ve mac dinh`() {
        PRMLocale.configure("!!!")

        assertEquals("vi", PRMLocale.current().language)
    }

    /**
     * Đây là vế "dấu phân cách nghìn theo locale" của CODE-3: cùng một số, hai ngôn ngữ, hai cách
     * hiển thị. Trước đây `formatDiscountAmount` tự chèn `"."` nên vế này luôn sai ngoài vi-VN.
     */
    @Test
    fun `dau phan cach nghin doi theo ngon ngu`() {
        PRMLocale.configure("vi-VN")
        val vietnamese = NumberFormat.getIntegerInstance(PRMLocale.current()).format(75_000)

        PRMLocale.configure("en-US")
        val english = NumberFormat.getIntegerInstance(PRMLocale.current()).format(75_000)

        assertEquals("75.000", vietnamese)
        assertEquals("75,000", english)
    }
}
