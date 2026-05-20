package com.ttcn.promotionsdk.ui.utils.extension

import android.graphics.Color
import androidx.annotation.ColorInt

object TokenColorParser {

    @ColorInt
    fun parse(hex: String?): Int? {
        if (hex.isNullOrBlank()) return null
        val normalized = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
        return runCatching { Color.parseColor(normalized) }.getOrNull()
    }
}
