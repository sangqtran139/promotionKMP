package com.ttcn.promotionsdk.di.internal

import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.common.SdkLock
import com.ttcn.promotionsdk.common.withLock
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

internal fun named(name: String): String = name

internal inline fun <reified T : Any> inject(qualifier: String? = null): Lazy<T> {
    return lazy {
        SdkDi.getInstance().resolve(T::class, qualifier)
    }
}

internal inline fun <reified T : Any> get(qualifier: String? = null): T {
    return SdkDi.getInstance().resolve(T::class, qualifier)
}

internal inline fun <reified T : Any> single(
    qualifier: String? = null,
    noinline definition: () -> T
) {
    SdkDi.getInstance().single(qualifier, definition)
}

internal inline fun <reified T : Any> factory(
    qualifier: String? = null,
    noinline definition: () -> T
) {
    SdkDi.getInstance().factory(qualifier, definition)
}

internal typealias Definition = () -> Unit

internal class Module(val definition: Definition)

internal fun module(definition: Definition): Module {
    return Module(definition)
}

internal class SdkDi {

    val registry = ComponentRegistry()

    inline fun <reified T : Any> single(qualifier: String? = null, noinline definition: () -> T) {
        registry.single(qualifier, definition)
    }

    inline fun <reified T : Any> factory(qualifier: String? = null, noinline definition: () -> T) {
        registry.factory(qualifier, definition)
    }

    fun <T : Any> resolve(clazz: KClass<T>, qualifier: String? = null): T {
        return registry.resolve(clazz, qualifier)
    }

    fun <T : Any> hasInstance(clazz: KClass<T>, qualifier: String? = null): Boolean {
        return registry.hasInstance(clazz, qualifier)
    }

    /**
     * Bản Android nhận thêm `Context` và đăng ký nó vào registry; ở common không còn `Context` nên
     * chỉ register [config] trước, sau đó invoke modules — thứ tự giữ nguyên.
     */
    fun start(config: PromotionSDKConfig, vararg modules: Module) {
        registry.single { config }
        loadModules(*modules)
    }

    fun loadModules(vararg modules: Module) {
        modules.forEach {
            it.definition.invoke()
        }
    }

    fun clear() {
        lock.withLock {
            registry.clear()
            instance = null
        }
    }

    companion object Companion {

        private val lock = SdkLock()

        @Volatile
        private var instance: SdkDi? = null

        fun getInstance(): SdkDi {
            return instance ?: lock.withLock {
                instance ?: SdkDi().also { instance = it }
            }
        }
    }
}
