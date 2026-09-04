package vn.viettelpay.networkkit

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.JsonConvertException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.IOException

/**
 * Bọc một lệnh gọi network, chuẩn hoá mọi lỗi transport thành [NetworkError]. Thứ tự `catch` quan
 * trọng: ba loại timeout của Ktor đều là con của [IOException] nên phải bắt trước — cùng thứ tự
 * `apiCall {}` của `PromotionRemoteDataSource` đã dùng (xem NetworkingGuide.md §5). Không bao giờ
 * nuốt [CancellationException] (CodingStandards §4).
 */
public suspend fun <T> networkCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: NetworkError) {
        throw e
    } catch (e: ResponseException) {
        throw NetworkError.Http(
            status = e.response.status.value,
            rawBody = runCatching { e.response.bodyAsText() }.getOrNull(),
            cause = e,
        )
    } catch (e: HttpRequestTimeoutException) {
        throw NetworkError.Timeout(e.message, e)
    } catch (e: ConnectTimeoutException) {
        throw NetworkError.Timeout(e.message, e)
    } catch (e: SocketTimeoutException) {
        throw NetworkError.Timeout(e.message, e)
    } catch (e: JsonConvertException) {
        throw NetworkError.Serialization(e)
    } catch (e: IOException) {
        throw NetworkError.NoConnection(e.message, e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw NetworkError.Unknown(e)
    }
