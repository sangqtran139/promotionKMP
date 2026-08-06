package com.ttcn.promotionsdk.presentation.promotiondetail

/**
 * Bọc nội dung HTML thô của 2 tab màn "Chi tiết ưu đãi" ("Thông tin chi tiết" / "Hướng dẫn sử dụng")
 * thành **một trang HTML hoàn chỉnh**, để WebView hai nền tảng render **giống hệt nhau**.
 * Cả hai nạp cùng chuỗi này (Android `WebView`, iOS `WKWebView`); cỡ chữ đặt bằng `<style>` nội tuyến.
 *
 * **Chuẩn hoá width** — 4 phép thay thế: nội dung do CMS sinh hay kèm `width` cố định theo `pt` làm
 * tràn ngang màn hình điện thoại → gỡ bỏ / đổi sang `word-wrap`.
 *
 * @return chuỗi rỗng khi [content] rỗng/trắng — native hiển thị trang trống (không có chữ mặc định).
 */
fun wrapPromotionHtml(content: String): String {
    if (content.isBlank()) return ""
    val normalized = content
        .replace("style=\"width:(| )\\w{1,}pt;\"".toRegex(), " ")
        .replace("width=\"\\w{1,}\"".toRegex(), " ")
        .replace("width:\\w{1,}pt".toRegex(), "word-wrap: break-word")
        .replace("width: \\w{1,}pt".toRegex(), "word-wrap: break-word")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>body { font-size: 14px; }</style>
        </head>
        <body>
        $normalized
        </body>
        </html>
    """.trimIndent()
}
