package vn.viettelpay.networkkit

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class NetworkKitStatusCodeTest {

    // Hai envelope KHÁC HÌNH DẠNG, mô phỏng đúng tình huống EkycStatusCodeHandler (network-kit-android)
    // vs ApiStatusHandler (miniapp) — mỗi bên tự có adapter toBusinessStatus() riêng, nhưng dùng
    // CHUNG một StatusCodeHandlerChain, không chép tay logic khớp code.
    private data class EkycStyleResponse(val statusCode: String?, val statusMessage: String?, val data: String?)

    private fun EkycStyleResponse.toBusinessStatus(): BusinessStatus? =
        statusCode?.let { code -> object : BusinessStatus {
            override val code = code
            override val message = statusMessage
        } }

    private data class MiniAppStyleResponse(val resultCode: String?, val resultMessage: String?)

    private fun MiniAppStyleResponse.toBusinessStatus(): BusinessStatus? =
        resultCode?.let { code -> object : BusinessStatus {
            override val code = code
            override val message = resultMessage
        } }

    private val sessionExpiredChain = StatusCodeHandlerChain(StatusCodeHandler.of("09"))

    @Test
    fun matchedCodeThrowsBusinessErrorWithServerCodeAndMessage() = runTest {
        val response = EkycStyleResponse(statusCode = "09", statusMessage = "Session expired", data = null)

        val error = assertFailsWith<BusinessError> {
            response.checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() }
        }

        assertEquals("09", error.code)
        assertEquals("Session expired", error.message)
    }

    @Test
    fun unmatchedCodeReturnsOriginalValueUnchanged() = runTest {
        val response = EkycStyleResponse(statusCode = "00", statusMessage = "OK", data = "payload")

        val result = response.checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() }

        assertSame(response, result)
    }

    @Test
    fun nullBusinessStatusIsTreatedAsSuccess() = runTest {
        // toBusinessStatus() trả null (vd envelope không có field status) -> không có gì để so khớp,
        // coi như thành công, không được ném lỗi.
        val response = MiniAppStyleResponse(resultCode = null, resultMessage = null)

        val result = response.checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() }

        assertSame(response, result)
    }

    @Test
    fun twoConsumersWithDifferentEnvelopesShareTheSameHandlerWithoutDrift() = runTest {
        // Đúng bug đã phát hiện: EkycStatusCodeHandler/ApiStatusHandler chép tay gần giống nhau, lệch
        // nhau một chi tiết. Ở đây cả hai envelope dùng CHUNG một StatusCodeHandler.of("09") — hành vi
        // (code + message của lỗi) phải giống hệt nhau, không lệch theo từng consumer.
        val ekycResponse = EkycStyleResponse(statusCode = "09", statusMessage = "Phiên hết hạn", data = null)
        val miniAppResponse = MiniAppStyleResponse(resultCode = "09", resultMessage = "Phiên hết hạn")

        val ekycError = assertFailsWith<BusinessError> {
            ekycResponse.checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() }
        }
        val miniAppError = assertFailsWith<BusinessError> {
            miniAppResponse.checkBusinessStatus(sessionExpiredChain) { it.toBusinessStatus() }
        }

        assertEquals(ekycError.code, miniAppError.code)
        assertEquals(ekycError.message, miniAppError.message)
    }

    @Test
    fun customOnMatchThrowsConsumerSpecificException() = runTest {
        class DomainException(val errorCode: String) : RuntimeException(errorCode)

        val chain = StatusCodeHandlerChain(
            StatusCodeHandler.of(setOf("EXPIRED")) { status -> throw DomainException(status.code ?: "unknown") },
        )
        val response = EkycStyleResponse(statusCode = "EXPIRED", statusMessage = "x", data = null)

        val error = assertFailsWith<DomainException> {
            response.checkBusinessStatus(chain) { it.toBusinessStatus() }
        }

        assertEquals("EXPIRED", error.errorCode)
    }

    @Test
    fun onlyFirstMatchingHandlerInChainRuns() = runTest {
        val chain = StatusCodeHandlerChain(
            StatusCodeHandler.of(setOf("A")) { throw BusinessError("A", "first") },
            StatusCodeHandler.of(setOf("A", "B")) { status -> throw BusinessError(status.code, "second") },
        )
        val response = EkycStyleResponse(statusCode = "A", statusMessage = null, data = null)

        val error = assertFailsWith<BusinessError> {
            response.checkBusinessStatus(chain) { it.toBusinessStatus() }
        }

        assertEquals("first", error.message)
    }

    @Test
    fun onMatchCanReturnNormallyInsteadOfThrowing() = runTest {
        // onMatch KHÔNG bị ép phải throw — vd handler chỉ log/gọi refresh rồi coi như xong, không có
        // lỗi để báo lên trên. checkBusinessStatus vẫn trả nguyên response, không ném gì.
        var refreshCalled = false
        val chain = StatusCodeHandlerChain(
            StatusCodeHandler.of(setOf("09")) { refreshCalled = true },
        )
        val response = EkycStyleResponse(statusCode = "09", statusMessage = null, data = "payload")

        val result = response.checkBusinessStatus(chain) { it.toBusinessStatus() }

        assertSame(response, result)
        assertEquals(true, refreshCalled)
    }
}
