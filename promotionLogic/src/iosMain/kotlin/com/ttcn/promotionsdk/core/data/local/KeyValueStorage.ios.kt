package com.ttcn.promotionsdk.core.data.local

import platform.Foundation.NSUserDefaults

/**
 * Dùng một suite riêng (`promotion_sdk_prefs`) thay vì `standardUserDefaults`, để `clear()` không
 * xoá nhầm preference của app host — tương đương việc Android mở file prefs riêng.
 */
internal class UserDefaultsStorage : KeyValueStorage {

    private val defaults: NSUserDefaults =
        NSUserDefaults(suiteName = KeyValueStorage.PREFS_NAME) ?: NSUserDefaults.standardUserDefaults

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (contains(key)) defaults.boolForKey(key) else default

    override fun contains(key: String): Boolean = defaults.objectForKey(key) != null

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun clear() {
        defaults.dictionaryRepresentation().keys.forEach { key ->
            defaults.removeObjectForKey(key as String)
        }
    }
}

internal actual fun createKeyValueStorage(): KeyValueStorage = UserDefaultsStorage()
