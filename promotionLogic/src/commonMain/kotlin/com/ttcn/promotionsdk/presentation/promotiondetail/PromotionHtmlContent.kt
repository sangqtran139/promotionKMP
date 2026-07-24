package com.ttcn.promotionsdk.presentation.promotiondetail

/**
 * Bọc nội dung HTML thô của 2 tab màn "Chi tiết ưu đãi" ("Thông tin chi tiết" / "Hướng dẫn sử dụng")
 * thành **một trang HTML hoàn chỉnh**, để WebView hai nền tảng render **giống hệt nhau**.
 *
 * Trước đây mỗi bên làm một kiểu và lệch rõ với HTML phức tạp:
 * - Android: `WebView` + wrapper dựng tay + 4 phép chuẩn hoá width, kèm `<LINK href="detail_endow.css">`
 *   — link này **xưa nay chết**: `loadDataWithBaseURL(null, …)` khiến URL tương đối không resolve, và
 *   file css cũng không tồn tại trong repo. Nay thay bằng `<style>` nội tuyến (tương đương
 *   `WebSettings.defaultFontSize = 14` mà Android vẫn đặt).
 * - iOS: `UITextView` + `NSAttributedString(documentType: .html)` — không có wrapper, không chuẩn hoá
 *   width, và TextKit render bảng/list rất khác trình duyệt.
 *
 * Nay cả hai nạp **cùng một chuỗi** này vào WebView (Android `WebView`, iOS `WKWebView`).
 *
 * **Chuẩn hoá width** giữ nguyên đúng 4 phép thay thế của bản Android: nội dung do CMS sinh hay kèm
 * `width` cố định theo `pt` làm tràn ngang màn hình điện thoại → gỡ bỏ / đổi sang `word-wrap`.
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
