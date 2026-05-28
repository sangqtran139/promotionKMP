// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/di/internal/ComponentRegistry.kt
package com.ttcn.promotionsdk.core.di.internal

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal class ComponentRegistry {

    private val singleInstances = ConcurrentHashMap<DiKey, Any>()
    private val providers = ConcurrentHashMap<DiKey, Provider<Any>>()
    private val factoryTypes = ConcurrentHashMap.newKeySet<DiKey>()

    inline fun <reified T : Any> single(qualifier: String? = null, noinline definition: () -> T) {
        val key = DiKey(T::class, qualifier)
        providers[key] = definition as Provider<Any>
    }

    inline fun <reified T : Any> factory(qualifier: String? = null, noinline definition: () -> T) {
        val key = DiKey(T::class, qualifier)
        providers[key] = definition as Provider<Any>
        factoryTypes.add(key)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(clazz: KClass<T>, qualifier: String? = null): T {
        val key = DiKey(clazz, qualifier)
        val provider = providers[key]
            ?: throw IllegalStateException(
                "Dependency not found: ${clazz.simpleName} (Qualifier: ${qualifier ?: "null"})"
            )

        if (factoryTypes.contains(key)) {
            return provider() as T
        }

        singleInstances[key]?.let {
            return it as T
        }

        synchronized(this) {
            singleInstances[key]?.let {
                return it as T
            }

            val instance = provider()
            singleInstances[key] = instance
            return instance as T
        }
    }

    fun <T : Any> contain(clazz: KClass<T>, qualifier: String? = null): Boolean {
        val key = DiKey(clazz, qualifier)
        return singleInstances.containsKey(key) || providers.containsKey(key)
    }

    fun clear() {
        singleInstances.clear()
        providers.clear()
        factoryTypes.clear()
    }
}