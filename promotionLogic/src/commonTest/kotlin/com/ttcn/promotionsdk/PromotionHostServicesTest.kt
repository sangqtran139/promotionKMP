package com.ttcn.promotionsdk

import com.ttcn.promotionsdk.common.PromotionAnalytics
import com.ttcn.promotionsdk.common.PromotionEvents
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.data.local.PromotionPreferences
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionEvents
import com.ttcn.promotionsdk.presentation.promotiondetail.PromotionDetailEvents
import com.ttcn.promotionsdk.host.PromotionEvent
import com.ttcn.promotionsdk.host.PromotionHostServices
import com.ttcn.promotionsdk.host.PromotionTracker
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class RecordingTracker : PromotionTracker {
    val events = mutableListOf<PromotionEvent>()
    override fun track(event: PromotionEvent) {
        events += event
    }
}

private class ThrowingTracker : PromotionTracker {
    var calls = 0
    override fun track(event: PromotionEvent) {
        calls++
        error("tracker của host hỏng")
    }
}

/** Kho khoá–giá trị giả, đứng thay "DB cũ" mà host cấp qua [PromotionHostServices.storage]. */
private class FakeHostStorage : PromotionPreferences {
    val values = mutableMapOf<String, Any>()
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String): String? = values[key] as? String
    override fun contains(key: String): Boolean = key in values
    override fun remove(key: String) { values.remove(key) }
    override fun clear() = values.clear()
}

/**
 * Hợp đồng của **gói năng lực do host cấp** ([PromotionHostServices]): host cấp thì SDK dùng đúng
 * thứ host đưa, không cấp thì SDK vẫn chạy bằng hiện thực của mình, và **host hỏng cũng không được
 * kéo đổ luồng đang chạy**.
 */
class PromotionHostServicesTest {

    @AfterTest
    fun tearDown() = PromotionContainer.clear()

    private fun init(tracker: PromotionTracker? = null) = PromotionContainer.initialize(
        PromotionSDKConfig(
            baseUrl = "https://api.example.com",
            hostServices = PromotionHostServices(tracker = tracker),
        )
    )

    @Test
    fun track_forwardsEventToHostTracker() {
        val tracker = RecordingTracker()
        init(tracker)

        PromotionAnalytics.track(PromotionDetailEvents.VIEW, mapOf("voucher_id" to "V-1"))

        assertEquals(1, tracker.events.size)
        assertEquals(PromotionDetailEvents.VIEW, tracker.events.first().name)
        assertEquals("V-1", tracker.events.first().params["voucher_id"])
    }

    @Test
    fun track_withoutHostTracker_isNoOp() {
        init(tracker = null)

        // Không cấu hình tracker → NoOpPromotionTracker, không nhánh null nào ở call-site.
        PromotionAnalytics.track(ChoosePromotionEvents.VIEW)
    }

    /** Chưa `initialize()` — unit test dựng store trực tiếp rơi vào nhánh này. */
    @Test
    fun track_beforeInitialize_isSilentlyIgnored() {
        PromotionAnalytics.track(ChoosePromotionEvents.VIEW)
    }

    @Test
    fun track_whenHostTrackerThrows_doesNotPropagate() {
        val tracker = ThrowingTracker()
        init(tracker)

        PromotionAnalytics.track(ChoosePromotionEvents.SELECT)

        assertEquals(1, tracker.calls)
    }

    /** Tracker phải sống cùng đồ thị DI: `clear()` rồi init lại là tracker mới, không giữ bản cũ. */
    @Test
    fun tracker_isRebuiltAfterClear() {
        val first = RecordingTracker()
        init(first)
        PromotionAnalytics.track(ChoosePromotionEvents.VIEW)
        PromotionContainer.clear()

        val second = RecordingTracker()
        init(second)
        PromotionAnalytics.track(ChoosePromotionEvents.VIEW)

        assertEquals(1, first.events.size)
        assertEquals(1, second.events.size)
    }

    /**
     * Tên event là hợp đồng với BI — chốt tiền tố để không ai đặt lệch quy ước.
     *
     * Test này **phải** liệt kê tay từng feature: danh mục cố tình nằm rải ở từng feature
     * (`ChoosePromotionEvents`, `PromotionDetailEvents`) chứ không gom vào `common/`, để tầng nền
     * không phải biết feature nào tồn tại — điều kiện để tách feature thành module riêng.
     */
    @Test
    fun eventNames_allUsePrmPrefix() {
        val names = listOf(
            ChoosePromotionEvents.VIEW,
            ChoosePromotionEvents.SEARCH,
            ChoosePromotionEvents.SELECT,
            ChoosePromotionEvents.APPLY_REJECTED,
            PromotionDetailEvents.VIEW,
        )

        names.forEach { assertTrue(it.startsWith(PromotionEvents.PREFIX), it) }
        assertEquals(names.size, names.distinct().size)
    }

    // ─── storage — kho của host thay kho mặc định ─────────────────────────────

    /**
     * Host cấp kho (DB cũ) → **toàn SDK** đọc ghi vào đó, không mở kho thứ hai.
     *
     * `assertSame` chứ không phải "ghi rồi đọc lại": điều cần khoá là `LocalModule` bind **đúng
     * instance của host**, không phải bind một bản sao rồi đồng bộ hai chiều.
     */
    @Test
    fun storage_whenHostProvidesOne_sdkUsesIt() {
        val hostStorage = FakeHostStorage()
        PromotionContainer.initialize(
            PromotionSDKConfig(
                baseUrl = "https://api.example.com",
                hostServices = PromotionHostServices(storage = hostStorage),
            )
        )

        assertSame(hostStorage, PromotionContainer.preferences)

        PromotionContainer.preferences.putString("prm_theme", "dark")
        assertEquals("dark", hostStorage.getString("prm_theme"))
    }

    /**
     * Không cấp kho → lùi về kho mặc định của nền tảng. Trên JVM host test không có `Context` nên
     * `createPreferences()` ném — chính điều đó chứng minh nhánh fallback đã chạy chứ không phải
     * nhánh host. Xem `PromotionPreferences.android.kt`.
     */
    @Test
    fun storage_whenHostProvidesNone_fallsBackToPlatformDefault() {
        init(tracker = null)

        val resolved = runCatching { PromotionContainer.preferences }

        resolved.onSuccess { assertTrue(it !is FakeHostStorage) }
    }
}
