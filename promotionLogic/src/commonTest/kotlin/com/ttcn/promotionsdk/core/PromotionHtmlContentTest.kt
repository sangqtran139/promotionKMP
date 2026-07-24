package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.presentation.promotiondetail.wrapPromotionHtml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `wrapPromotionHtml` là nguồn HTML **dùng chung** cho WebView hai nền tảng (Android `WebView`,
 * iOS `WKWebView`). Lệch ở đây là lệch hiển thị màn Chi tiết trên cả hai bên cùng lúc.
 */
class PromotionHtmlContentTest {

    @Test
    fun blankContent_returnsEmptyString_soNativeShowsBlankPage() {
        assertEquals("", wrapPromotionHtml(""))
        assertEquals("", wrapPromotionHtml("   \n\t "))
    }

    @Test
    fun wrapsContentInFullHtmlPage_withViewportAndBaseFontSize() {
        val html = wrapPromotionHtml("<p>Xin chào</p>")

        assertTrue(html.startsWith("<!DOCTYPE html>"), "phải là trang HTML hoàn chỉnh: $html")
        assertTrue(html.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"))
        // Thay cho detail_endow.css (link cũ không bao giờ resolve vì baseURL null + file không tồn tại).
        assertTrue(html.contains("font-size: 14px"))
        assertTrue(html.contains("<p>Xin chào</p>"), "nội dung gốc phải được giữ nguyên")
        assertTrue(html.trimEnd().endsWith("</html>"))
    }

    @Test
    fun stripsFixedPixelWidths_soCmsContentDoesNotOverflow() {
        val html = wrapPromotionHtml(
            """<table style="width:600pt;"><tr><td width="320">ô</td></tr></table>"""
        )

        assertFalse(html.contains("style=\"width:600pt;\""), "width cố định theo pt phải bị gỡ: $html")
        assertFalse(html.contains("width=\"320\""), "thuộc tính width cố định phải bị gỡ: $html")
        assertTrue(html.contains("ô"), "nội dung trong ô phải còn")
    }

    @Test
    fun rewritesInlineWidthDeclarations_toWordWrap() {
        // Cả hai biến thể có/không khoảng trắng sau dấu hai chấm đều phải đổi.
        assertTrue(wrapPromotionHtml("<div style=\"width:480pt\">a</div>").contains("word-wrap: break-word"))
        assertTrue(wrapPromotionHtml("<div style=\"width: 480pt\">b</div>").contains("word-wrap: break-word"))
    }
}
