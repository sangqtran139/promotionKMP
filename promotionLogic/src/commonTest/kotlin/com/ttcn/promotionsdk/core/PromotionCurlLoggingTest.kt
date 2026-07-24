package com.ttcn.promotionsdk.core

import com.ttcn.promotionsdk.core.data.remote.buildCurlCommand
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lệnh cURL chỉ là log chẩn đoán, nhưng nó phải **dán chạy được**: sai escape hay thiếu body là dev
 * đuổi theo một request không tồn tại. Test đi qua đủ các nhánh dựng chuỗi.
 */
class PromotionCurlLoggingTest {

    private fun request(
        method: HttpMethod = HttpMethod.Get,
        url: String = "https://api.test/v1/vouchers",
        headers: Map<String, String> = emptyMap(),
    ) = HttpRequestBuilder().apply {
        this.method = method
        url(url)
        headers.forEach { (name, value) -> this.headers.append(name, value) }
    }

    @Test
    fun getWithoutBody_hasMethodHeadersAndUrl_butNoDataFlag() {
        val curl = buildCurlCommand(
            request(headers = mapOf("Authorization" to "Bearer abc", "X-Request-ID" to "req-1")),
            body = null,
        )

        assertTrue(curl.contains("curl -X GET"), curl)
        assertTrue(curl.contains("-H 'Authorization: Bearer abc'"), curl)
        assertTrue(curl.contains("-H 'X-Request-ID: req-1'"), curl)
        assertTrue(curl.contains("'https://api.test/v1/vouchers'"), curl)
        assertFalse(curl.contains("-d "), "không có body thì không được sinh -d: $curl")
    }

    @Test
    fun postWithJsonBody_addsContentTypeFromBody_andPayload() {
        val curl = buildCurlCommand(
            request(method = HttpMethod.Post),
            body = TextContent("""{"orderId":"O1"}""", ContentType.Application.Json),
        )

        assertTrue(curl.contains("curl -X POST"), curl)
        // Content-Type nằm trên OutgoingContent, không ở headers builder.
        assertTrue(curl.contains("-H 'Content-Type: application/json"), curl)
        assertTrue(curl.contains("""-d '{"orderId":"O1"}'"""), curl)
    }

    @Test
    fun payloadWithSingleQuote_isEscapedForShell() {
        val curl = buildCurlCommand(
            request(method = HttpMethod.Post),
            body = TextContent("""{"note":"it's ok"}""", ContentType.Application.Json),
        )

        // ' -> '\'' để chuỗi vẫn đóng/mở đúng khi dán vào shell.
        assertTrue(curl.contains("""it'\''s ok"""), curl)
    }

    @Test
    fun byteArrayBody_isDecodedIntoDataFlag() {
        val binary = object : OutgoingContent.ByteArrayContent() {
            override val contentType = ContentType.Application.OctetStream
            override fun bytes() = "raw-payload".encodeToByteArray()
        }

        val curl = buildCurlCommand(request(method = HttpMethod.Post), body = binary)

        assertTrue(curl.contains("-d 'raw-payload'"), curl)
        assertTrue(curl.contains("-H 'Content-Type: application/octet-stream"), curl)
    }

    @Test
    fun emptyTextBody_doesNotEmitDataFlag() {
        val curl = buildCurlCommand(
            request(method = HttpMethod.Post),
            body = TextContent("", ContentType.Application.Json),
        )

        assertFalse(curl.contains("-d "), "body rỗng thì bỏ qua -d: $curl")
    }

    @Test
    fun bodyThatIsNotOutgoingContent_isIgnored() {
        // Chặn ở Monitoring nên body thường đã là OutgoingContent; nếu không thì bỏ qua, không nổ.
        val curl = buildCurlCommand(request(method = HttpMethod.Post), body = "chuỗi thô")

        assertFalse(curl.contains("-d "), curl)
        assertFalse(curl.contains("Content-Type"), curl)
        assertTrue(curl.contains("curl -X POST"), curl)
    }
}
