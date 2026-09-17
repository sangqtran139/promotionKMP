package com.ttcn.prm.entry.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hợp đồng của bề mặt headless khi SDK **chưa `initialize()`**: mọi hàm trả
 * [PromotionSDKError.NotInitialized], không ném, không chạm mạng.
 *
 * Vì sao đáng khoá bằng test: `PromotionSDKApi.notInitialized` chặn bằng **một** chỗ duy nhất —
 * `isReady = false` kiểm ở hàm `handle()` nội bộ, và cả 5 hàm public đều phải đi qua đó. Giả định
 * này không được compiler bảo vệ: thêm một hàm public mới mà quên bọc `handle` thì nó sẽ chạy thẳng
 * vào `useCases`, chạm `lazy { error("unreachable: isReady = false") }` và **ném ra tận host**.
 * KDoc của `notInitialized` có cảnh báo điều đó nhưng cảnh báo không chặn được ai.
 *
 * Từ khi host được phép **cố ý** không init (cờ tắt tính năng ở phía host), đây không còn là ca
 * hiếm lúc khởi động nữa mà là trạng thái chạy bình thường — nên nó cần test thật.
 *
 * Kotlin thuần, không đụng Android → chạy bằng unit test JVM, không cần Robolectric.
 * Dùng `runBlocking` chứ không `runTest`: module này không có `kotlinx-coroutines-test` trên
 * classpath, và các hàm ở đây thoát ngay ở `handle()` nên không có delay nào cần điều khiển.
 */
class PromotionSDKApiNotInitializedTest {

    private val api = PromotionSDKApi.notInitialized

    private fun assertNotInitialized(result: PromotionApiResult<*>) {
        val failure = result as? PromotionApiResult.Failure
            ?: error("Mong đợi Failure, nhận: $result")
        assertEquals(PromotionSDKError.NotInitialized, failure.error)
    }

    @Test
    fun `getVouchers khi chua init tra NotInitialized`() = runBlocking {
        assertNotInitialized(api.getVouchers())
    }

    @Test
    fun `findEligible khi chua init tra NotInitialized`() = runBlocking {
        assertNotInitialized(api.findEligible(orderId = "o-1", orderValue = "500000"))
    }

    @Test
    fun `getVoucherDetail khi chua init tra NotInitialized`() = runBlocking {
        assertNotInitialized(api.getVoucherDetail(voucherId = "v-1"))
    }

    @Test
    fun `validateDiscounts khi chua init tra NotInitialized`() = runBlocking {
        assertNotInitialized(api.validateDiscounts(orderId = "o-1", orderValue = "500000", voucherIds = listOf("v-1")))
    }

    @Test
    fun `createRedemption khi chua init tra NotInitialized`() = runBlocking {
        assertNotInitialized(api.createRedemption(orderId = "o-1", orderValue = "500000", voucherIds = listOf("v-1")))
    }

    /**
     * `NotInitialized` là `data object` nên so sánh bằng identity được — host `when` trên nó mà
     * không cần `is`. Đổi nó thành `data class` sẽ phá cách host đang bắt lỗi.
     */
    @Test
    fun `NotInitialized la singleton so sanh duoc bang dang thuc`() {
        assertTrue(PromotionSDKError.NotInitialized === PromotionSDKError.NotInitialized)
    }
}
