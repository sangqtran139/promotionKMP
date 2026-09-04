package vn.viettelpay.networkkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NetworkKitJsonTest {

    @Serializable
    data class SampleDto(
        val id: Int,
        val name: String,
        val amount: String,
        val note: String = "no-note",
        val altId: String?,
    )

    @Test
    fun getJsonDecodesMatchingResponseBody() = runTest {
        val client = jsonClient("""{"id":1,"name":"Voucher A","amount":"500000","altId":"alt-1"}""")

        val result: SampleDto = client.getJson("sample")

        assertEquals(SampleDto(id = 1, name = "Voucher A", amount = "500000", altId = "alt-1"), result)
    }

    @Test
    fun getJsonToleratesUnknownFieldsInResponse() = runTest {
        val client = jsonClient(
            """{"id":1,"name":"Voucher A","amount":"500000","extraField":"ignored"}""",
        )

        val result: SampleDto = client.getJson("sample")

        assertEquals("Voucher A", result.name)
    }

    @Test
    fun getJsonAcceptsNumericValueForStringField() = runTest {
        // Bẫy ghi trong NetworkingGuide.md: server trả số cho field tiền tệ khai kiểu String
        // ("originalAmount": 500000) — mặc định kotlinx.serialization ném lỗi, `isLenient = true`
        // (cài trong NetworkKitHttpClient) mới chấp nhận.
        val client = jsonClient("""{"id":1,"name":"Voucher A","amount":500000}""")

        val result: SampleDto = client.getJson("sample")

        assertEquals("500000", result.amount)
    }

    @Test
    fun getJsonThrowsOnMissingRequiredFieldInsteadOfFailingSilently() = runTest {
        val client = jsonClient("""{"id":1}""")

        // Ktor bọc MissingFieldException (kotlinx.serialization) vào JsonConvertException của riêng
        // nó — đây là loại lỗi caller thực sự thấy ở `.body<T>()`. `name`/`amount` không có default
        // nên đúng nghĩa "bắt buộc": thiếu là lỗi, không phải giá trị rỗng âm thầm.
        assertFailsWith<JsonConvertException> {
            client.getJson<SampleDto>("sample")
        }
    }

    @Test
    fun getJsonUsesDefaultValueWhenOptionalFieldIsMissing() = runTest {
        // note: String = "no-note" CÓ default -> không bắt buộc. Server không trả field này (thiếu
        // hẳn khỏi JSON, không phải null) thì decode phải dùng default, không được ném lỗi.
        val client = jsonClient("""{"id":1,"name":"Voucher A","amount":"500000"}""")

        val result: SampleDto = client.getJson("sample")

        assertEquals("no-note", result.note)
    }

    @Test
    fun getJsonUsesServerValueWhenOptionalFieldIsPresent() = runTest {
        // Ngược lại: server CÓ trả field có default thì phải dùng giá trị server, không phải lặng lẽ
        // rơi về default.
        val client = jsonClient(
            """{"id":1,"name":"Voucher A","amount":"500000","note":"server note"}""",
        )

        val result: SampleDto = client.getJson("sample")

        assertEquals("server note", result.note)
    }

    @Test
    fun getJsonTreatsMissingNullableFieldWithoutDefaultAsNull() = runTest {
        // altId: String? KHÔNG có default — khác note ở trên (có default). Server thiếu hẳn field
        // này trong JSON (không phải "altId":null). Mặc định kotlinx.serialization sẽ ném
        // MissingFieldException dù field nullable, vì thiếu != null tường minh. `explicitNulls =
        // false` (cài trong NetworkKitHttpClient) mới cho phép coi vắng mặt = null.
        val client = jsonClient("""{"id":1,"name":"Voucher A","amount":"500000"}""")

        val result: SampleDto = client.getJson("sample")

        assertNull(result.altId)
    }

    private fun jsonClient(responseBody: String): HttpClient {
        val engine = MockEngine {
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val config = NetworkClientConfig(baseUrl = "https://api.example.com/base")
        return HttpClient(engine) {
            with(NetworkKitHttpClient) { configure(config) }
        }
    }
}
