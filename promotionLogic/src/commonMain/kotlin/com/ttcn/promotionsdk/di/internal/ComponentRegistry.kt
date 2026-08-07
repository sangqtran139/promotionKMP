package com.ttcn.promotionsdk.di.internal

import com.ttcn.promotionsdk.common.SdkLock
import com.ttcn.promotionsdk.common.withLock
import kotlin.reflect.KClass

/**
 * Bản đa nền tảng của registry. `ConcurrentHashMap` + `synchronized` là JVM-only nên mọi truy cập
 * map được bảo vệ bằng [SdkLock] (reentrant) — giữ nguyên ngữ nghĩa double-checked locking của
 * [resolve].
 */
internal class ComponentRegistry {

    private val lock = SdkLock()
    private val singleInstances = mutableMapOf<DiKey, Any>()
    private val providers = mutableMapOf<DiKey, Provider<Any>>()
    private val factoryTypes = mutableSetOf<DiKey>()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> single(qualifier: String? = null, noinline definition: () -> T) {
        register(DiKey(T::class, qualifier), definition as Provider<Any>, isFactory = false)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> factory(qualifier: String? = null, noinline definition: () -> T) {
        register(DiKey(T::class, qualifier), definition as Provider<Any>, isFactory = true)
    }

    fun register(key: DiKey, provider: Provider<Any>, isFactory: Boolean) {
        lock.withLock {
            providers[key] = provider
            if (isFactory) factoryTypes.add(key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(clazz: KClass<T>, qualifier: String? = null): T {
        val key = DiKey(clazz, qualifier)

        return lock.withLock {
            val provider = providers[key]
                ?: throw IllegalStateException(
                    "Dependency not found: ${clazz.simpleName} (Qualifier: ${qualifier ?: "null"})"
                )

            if (factoryTypes.contains(key)) {
                return@withLock provider() as T
            }

            singleInstances[key]?.let { return@withLock it as T }

            val instance = provider()
            singleInstances[key] = instance
            instance as T
        }
    }

    fun <T : Any> contain(clazz: KClass<T>, qualifier: String? = null): Boolean {
        val key = DiKey(clazz, qualifier)
        return lock.withLock { singleInstances.containsKey(key) || providers.containsKey(key) }
    }

    /** Đã dựng instance hay chưa — khác [contain], vốn true ngay khi mới đăng ký provider. */
    fun <T : Any> hasInstance(clazz: KClass<T>, qualifier: String? = null): Boolean =
        lock.withLock { singleInstances.containsKey(DiKey(clazz, qualifier)) }

    fun clear() {
        lock.withLock {
            singleInstances.clear()
            providers.clear()
            factoryTypes.clear()
        }
    }
}
