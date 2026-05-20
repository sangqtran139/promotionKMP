package com.ttcn.promotionsdk.ui.theme

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object PromotionThemeJson {

    private val gson: Gson = GsonBuilder().create()

    fun toJson(config: PromotionThemeConfig): String = gson.toJson(config)

    fun fromJson(json: String): PromotionThemeConfig? =
        runCatching { gson.fromJson(json, PromotionThemeConfig::class.java) }.getOrNull()
}
