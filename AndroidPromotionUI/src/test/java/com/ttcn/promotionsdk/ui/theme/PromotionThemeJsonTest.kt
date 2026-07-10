package com.ttcn.promotionsdk.ui.theme

import com.ttcn.promotionsdk.ui.theme.token.ButtonToken
import com.ttcn.promotionsdk.ui.theme.token.ListItemToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bảo vệ **định dạng JSON dùng chung Android ↔ iOS**. Đổi key hoặc đổi vị trí alpha ở một bên mà
 * quên bên kia thì theme sẽ sai màu trong im lặng — xem KDoc của [PromotionThemeJson].
 */
class PromotionThemeJsonTest {

    // ─── ThemeHex ─────────────────────────────────────────────────────────────

    @Test
    fun `mau duc rut gon con 6 ky tu`() {
        assertEquals("#EE0033", ThemeHex.format(0xFFEE0033.toInt()))
    }

    @Test
    fun `mau co alpha giu 8 ky tu, alpha dung truoc`() {
        assertEquals("#80EE0033", ThemeHex.format(0x80EE0033.toInt()))
    }

    @Test
    fun `parse 6 ky tu coi la duc`() {
        assertEquals(0xFFEE0033.toInt(), ThemeHex.parse("#EE0033"))
    }

    @Test
    fun `parse 8 ky tu doc alpha o dau`() {
        assertEquals(0x80EE0033.toInt(), ThemeHex.parse("#80EE0033"))
    }

    @Test
    fun `parse chap nhan chuoi khong co dau thang`() {
        assertEquals(0xFFEE0033.toInt(), ThemeHex.parse("EE0033"))
    }

    @Test
    fun `parse tra null voi chuoi hong`() {
        assertNull(ThemeHex.parse(null))
        assertNull(ThemeHex.parse(""))
        assertNull(ThemeHex.parse("#XYZ"))
        assertNull(ThemeHex.parse("#EE003"))       // 5 ky tu
        assertNull(ThemeHex.parse("#EE0033FFAA"))  // 10 ky tu
    }

    @Test
    fun `format roi parse lai ra dung mau ban dau`() {
        listOf(0xFFEE0033.toInt(), 0x00000000, 0x80123456.toInt(), 0xFFFFFFFF.toInt())
            .forEach { assertEquals(it, ThemeHex.parse(ThemeHex.format(it))) }
    }

    // ─── Hình dạng JSON ───────────────────────────────────────────────────────

    @Test
    fun `key nhom la button khong phai buttonToken`() {
        val json = PromotionThemeJson.toJson(
            PromotionSDKTheme(buttonToken = ButtonToken(backgroundColor = 0xFFEE0033.toInt()))
        )
        assertTrue(json, json.contains("\"button\""))
        assertTrue(json, !json.contains("buttonToken"))
    }

    @Test
    fun `mau serialize thanh chuoi hex chu khong phai so`() {
        val json = PromotionThemeJson.toJson(
            PromotionSDKTheme(buttonToken = ButtonToken(backgroundColor = 0xFFEE0033.toInt()))
        )
        assertTrue(json, json.contains("\"#EE0033\""))
        // Bug cu: Gson serialize thang Int? -> {"backgroundColor":-1179597}
        assertTrue(json, !json.contains("-1179597"))
    }

    @Test
    fun `nhom khong duoc set thi vang mat khoi json`() {
        val json = PromotionThemeJson.toJson(PromotionSDKTheme(buttonToken = ButtonToken()))
        assertTrue(json, !json.contains("searchBar"))
    }

    // ─── Round-trip ───────────────────────────────────────────────────────────

    @Test
    fun `round trip giu nguyen mau, corner radius va nhom null`() {
        val theme = PromotionSDKTheme(
            buttonToken = ButtonToken(
                backgroundColor = 0xFFEE0033.toInt(),
                textColor = 0x80FFFFFF.toInt(),
                cornerRadius = 8f,
            ),
            listItemToken = ListItemToken(
                radioButtonStrokeColor = 0xFF666666.toInt(),
                radioButtonSelectedStrokeColor = 0xFFEE0033.toInt(),
            ),
        )

        val back = PromotionThemeJson.fromJson(PromotionThemeJson.toJson(theme))

        assertEquals(theme, back)
        assertNull(back!!.searchBarToken)
        assertNull(back.tabChipToken)
    }

    @Test
    fun `doc duoc json do iOS sinh ra`() {
        // Đúng thứ mà `ThemeDTO` bên Swift ghi ra: key nhóm `button`/`listItem`, hex `#AARRGGBB`.
        val iosJson = """
            {
              "button": { "backgroundColor": "#FFEE0033", "cornerRadius": 8.0 },
              "listItem": { "radioButtonSelectedStrokeColor": "#EE0033" }
            }
        """.trimIndent()

        val theme = PromotionThemeJson.fromJson(iosJson)

        assertEquals(0xFFEE0033.toInt(), theme!!.buttonToken!!.backgroundColor)
        assertEquals(8f, theme.buttonToken!!.cornerRadius)
        assertEquals(0xFFEE0033.toInt(), theme.listItemToken!!.radioButtonSelectedStrokeColor)
        assertNull(theme.searchBarToken)
    }

    @Test
    fun `json hong tra null chu khong nem`() {
        assertNull(PromotionThemeJson.fromJson("{ khong phai json }"))
    }
}
