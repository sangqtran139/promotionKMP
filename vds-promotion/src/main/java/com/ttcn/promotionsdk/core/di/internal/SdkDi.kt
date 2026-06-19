package com.ttcn.promotionsdk.core.di.internal

import android.content.Context
import com.ttcn.promotionsdk.core.config.PromotionSDKConfig
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

    private val registry = ComponentRegistry()

    inline fun <reified T : Any> single(qualifier: String? = null, noinline definition: () -> T) {
        registry.single(qualifier, definition)
    }

    inline fun <reified T : Any> factory(qualifier: String? = null, noinline definition: () -> T) {
        registry.factory(qualifier, definition)
    }

    fun <T : Any> resolve(clazz: KClass<T>, qualifier: String? = null): T {
        return registry.resolve(clazz, qualifier)
    }

    // PromotionSDKConfig thay cho Configuration của bản gốc
    // Logic start() giữ nguyên: register context + config trước, sau đó invoke modules
    fun start(context: Context, config: PromotionSDKConfig, vararg modules: Module) {
        registry.single { context.applicationContext }
        registry.single { config }

        loadModules(*modules)
    }

    fun loadModules(vararg modules: Module) {
        modules.forEach {
            it.definition.invoke()
        }
    }

    fun clear() {
        synchronized(Companion) {
            registry.clear()
            instance = null
        }
    }

    companion object Companion {

        @Volatile
        private var instance: SdkDi? = null

        fun getInstance(): SdkDi {
            return instance
                ?: synchronized(this) {
                    instance ?: SdkDi().also { instance = it }
                }
        }
    }
}