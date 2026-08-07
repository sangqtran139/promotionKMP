package com.ttcn.promotionsdk

import com.russhwolf.settings.MapSettings
import com.ttcn.promotionsdk.data.local.SettingsPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `SettingsPreferences` là lớp mỏng bọc `Settings`, nhưng nó là hiện thực **duy nhất** cho cả hai nền
 * tảng — sai một chỗ là sai cả Android lẫn iOS. Test chạy ở `commonTest` nên kiểm trên cả hai target.
 *
 * Điểm dễ sai nhất và là lý do có file này: `getString` phải trả `null` khi thiếu key. `Settings` có
 * `getString(key, default)` trả chuỗi rỗng mặc định — dùng nhầm hàm đó thì `PromotionThemeStore.load()`
 * sẽ nhận `""` thay vì `null` rồi ném khi parse JSON. Phải là `getStringOrNull`.
 */
class PromotionPreferencesTest {

    private fun prefs() = SettingsPreferences(MapSettings())

    @Test
    fun getString_returnsNull_whenKeyAbsent() {
        assertNull(prefs().getString("promotion_theme_config_v1"))
    }

    @Test
    fun putString_thenGetString_roundTrips() {
        val prefs = prefs()

        prefs.putString("theme", """{"primary":"#FF0000"}""")

        assertEquals("""{"primary":"#FF0000"}""", prefs.getString("theme"))
        assertTrue(prefs.contains("theme"))
    }

    @Test
    fun getBoolean_returnsGivenDefault_whenKeyAbsent() {
        val prefs = prefs()

        assertTrue(prefs.getBoolean("missing", default = true))
        assertFalse(prefs.getBoolean("missing", default = false))
    }

    @Test
    fun putBoolean_thenGetBoolean_roundTrips() {
        val prefs = prefs()

        prefs.putBoolean("flag", false)

        // Giá trị đã lưu phải THẮNG default — nếu không, cờ tắt từ server sẽ bị đọc thành bật.
        assertFalse(prefs.getBoolean("flag", default = true))
    }

    @Test
    fun remove_dropsOnlyThatKey() {
        val prefs = prefs()
        prefs.putString("theme", "{}")
        prefs.putBoolean("flag", true)

        prefs.remove("theme")

        assertFalse(prefs.contains("theme"))
        assertNull(prefs.getString("theme"))
        assertTrue(prefs.contains("flag"))
    }

    @Test
    fun clear_dropsEverything() {
        val prefs = prefs()
        prefs.putString("theme", "{}")
        prefs.putBoolean("flag", true)

        prefs.clear()

        assertFalse(prefs.contains("theme"))
        assertFalse(prefs.contains("flag"))
    }
}
