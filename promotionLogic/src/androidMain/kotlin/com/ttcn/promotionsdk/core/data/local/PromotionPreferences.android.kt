package com.ttcn.promotionsdk.core.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlin.concurrent.Volatile

/**
 * Giữ `applicationContext` do `PromotionContainer.initialize(context, config)` nạp vào. Chỉ dùng để mở
 * `SharedPreferences` — SDK không giữ context nào khác.
 */
internal object AndroidContextHolder {

    @Volatile
    private var applicationContext: Context? = null

    fun set(context: Context) {
        applicationContext = context.applicationContext
    }

    fun clear() {
        applicationContext = null
    }

    fun require(): Context = requireNotNull(applicationContext) {
        "Promotion SDK chưa có Context. Trên Android hãy gọi PromotionContainer.initialize(context, config)."
    }
}

internal class SharedPrefStorage(context: Context) : PromotionPreferences {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PromotionPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

internal actual fun createPreferences(): PromotionPreferences =
    SharedPrefStorage(AndroidContextHolder.require())
