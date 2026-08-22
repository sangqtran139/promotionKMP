package com.ttcn.promotionsdk.data.remote

import com.ttcn.promotionsdk.common.SdkLock
import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Cổng refresh token: biến `PromotionRequestContextProvider.refreshAccessToken` (callback, do host
 * cài đặt ở tầng native) thành một lệnh `suspend` **single-flight** cho `PromotionRemoteDataSource`.
 *
 * ### Vì sao cần
 *
 * Token của host sống ~15 phút. `PromotionTokenSource.currentToken()` đã lo được ca thường gặp —
 * host lấy token mới xong trước khi SDK gọi API. Còn lại đúng một ca: token chết **giữa** lúc màn
 * hình SDK đang mở. Không có cổng này thì request đó hỏng, user thấy màn lỗi, và host chỉ biết
 * chuyện qua `onExpireToken()` — quá muộn để cứu lượt gọi đã hỏng.
 *
 * ### Single-flight
 *
 * Mở màn "Ưu đãi của tôi" là vài request bay song song; token chết thì **tất cả** cùng ăn 401. Để
 * host không bị gọi refresh nhiều lần cho cùng một sự kiện, mỗi lượt refresh thành công tăng
 * [generation] lên 1. Nơi gọi chụp [generation] **trước** khi gửi request; lúc thất bại nó đưa lại
 * số đã chụp:
 *
 * - số chụp **khác** số hiện tại ⇒ có người vừa refresh xong sau lưng nó ⇒ khỏi refresh, thử lại luôn.
 * - số chụp **bằng** số hiện tại ⇒ nó là request đầu tiên phát hiện ⇒ chính nó đi hỏi host.
 *
 * `Mutex` giữ cho phần so-sánh-rồi-refresh là một khối không chen ngang được, nên N request ăn 401
 * cùng lúc chỉ tốn **một** lần hỏi host.
 *
 * ### Vì sao chỉ trả `Boolean`
 *
 * Token đi vào request theo **một** đường duy nhất: `getAccessToken()`, mà `defaultRequest { }` hỏi
 * lại ở mỗi lần gửi. Lượt thử lại vì vậy tự nhặt được token mới, không cần ai chuyển chuỗi token qua
 * cổng này — nó chỉ cần biết **có** token mới hay không. Cho phép trả token ở đây là mở đường thứ
 * hai, kèm câu hỏi "SDK dùng chuỗi host trả hay đọc lại kho của host?".
 */
internal class TokenRefreshGate(
    private val contextProvider: PromotionRequestContextProvider,
) {

    private val mutex = Mutex()

    /** Tăng 1 sau **mỗi** lượt refresh thành công. Xem mô tả single-flight ở KDoc lớp. */
    @Volatile
    var generation: Int = 0
        private set

    /**
     * @param seenGeneration giá trị [generation] mà lượt gọi hỏng đã dùng.
     * @return `true` nếu đã có token mới và nơi gọi **nên** thử lại; `false` nếu host không lấy
     * được token mới (hoặc không cài đặt) — lúc đó để lỗi 401 nổi lên.
     */
    suspend fun refresh(seenGeneration: Int): Boolean = mutex.withLock {
        // Có người khác vừa refresh xong trong lúc mình đang chờ khoá → token đã mới, thử lại ngay.
        if (seenGeneration != generation) return@withLock true

        if (!awaitFreshToken()) return@withLock false
        generation++
        true
    }

    /**
     * Bọc callback của host thành `suspend`, với **hai lớp chắn** cho code ngoài tầm kiểm soát:
     *
     * - [withTimeoutOrNull] — host không bao giờ gọi lại `onResult` thì cổng này treo vĩnh viễn
     *   **cùng với `Mutex`**, tức mọi API của SDK chết theo. Hết giờ = coi như refresh hỏng.
     * - [SdkLock] + cờ `resumed` — host gọi `onResult` hai lần thì `Continuation.resume` lần hai
     *   ném `IllegalStateException` từ một thread lạ. Chỉ lần đầu được tính.
     *
     * @return `true` khi host báo đã có token mới. Hết giờ chờ → `false`.
     */
    private suspend fun awaitFreshToken(): Boolean = withTimeoutOrNull(REFRESH_TIMEOUT_MILLIS.milliseconds) {
        suspendCancellableCoroutine { continuation ->
            // `lock()`/`unlock()` trần thay cho `SdkLock.withLock`: trong file này đã có
            // `Mutex.withLock` của coroutines, hai extension trùng tên đọc lên rất dễ nhầm.
            val lock = SdkLock()
            var resumed = false
            contextProvider.refreshAccessToken { didRefresh ->
                lock.lock()
                val isFirst = !resumed
                resumed = true
                lock.unlock()
                if (isFirst) continuation.resume(didRefresh)
            }
        }
    } == true   // `null` = hết giờ chờ host → coi như không lấy được

    private companion object {
        /**
         * Ngắn hơn `PromotionHttpClient.TIMEOUT_MILLIS` (30s): refresh là **phần thêm** vào một
         * request vốn đã có ngân sách thời gian riêng, không được phép làm đôi thời gian chờ của user.
         */
        const val REFRESH_TIMEOUT_MILLIS = 15_000L
    }
}
