package com.ttcn.promotionsdk.core.data.remote

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent

/**
 * In mỗi request Ktor ra **lệnh cURL** copy-paste được (chỉ khi `isDebug`).
 *
 * Ktor `Logging` (LogLevel.BODY) in request/response theo format riêng của nó; plugin này bổ sung
 * dòng cURL để dev dán thẳng vào terminal / Postman khi đối chiếu với server thật. Chặn ở
 * [HttpSendPipeline.Monitoring] — chạy **sau** khi `ContentNegotiation` đã serialize body thành
 * [OutgoingContent] và `defaultRequest` đã gắn đủ header (Authorization/X-Request-ID/Accept…), nên
 * cURL phản ánh đúng byte đi ra dây.
 *
 * ⚠️ Giống LogLevel.BODY, cURL sẽ chứa cả header `Authorization` (Bearer token) — vì vậy chỉ bật
 * khi `isDebug`. Không dùng ở bản phát hành.
 */
internal val PromotionCurlLogging = createClientPlugin("PromotionCurlLogging") {
    client.sendPipeline.intercept(HttpSendPipeline.Monitoring) { content ->
        // Không để lỗi format cURL làm hỏng request thật — chỉ là log chẩn đoán.
        runCatching { println(buildCurlCommand(context, content)) }
    }
}

private fun buildCurlCommand(request: HttpRequestBuilder, body: Any?): String {
    val sb = StringBuilder("[PromotionSDK] cURL:\ncurl -X ").append(request.method.value)

    request.headers.entries().forEach { (name, values) ->
        values.forEach { value ->
            sb.append(" \\\n  -H '").append(name).append(": ").append(value).append('\'')
        }
    }

    if (body is OutgoingContent) {
        // Content-Type nằm trên OutgoingContent (do ContentNegotiation đặt), không ở headers builder.
        body.contentType?.let { sb.append(" \\\n  -H 'Content-Type: ").append(it.toString()).append('\'') }
        val payload = when (body) {
            is TextContent -> body.text
            is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
            else -> null
        }
        if (!payload.isNullOrEmpty()) {
            // Escape dấu nháy đơn cho shell: ' -> '\''
            sb.append(" \\\n  -d '").append(payload.replace("'", "'\\''")).append('\'')
        }
    }

    sb.append(" \\\n  '").append(request.url.buildString()).append('\'')
    return sb.toString()
}
