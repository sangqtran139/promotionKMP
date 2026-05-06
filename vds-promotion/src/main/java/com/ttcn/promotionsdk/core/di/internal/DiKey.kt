// vds-promotion/src/main/java/com/ttcn/promotionsdk/core/di/internal/DiKey.kt
package com.ttcn.promotionsdk.core.di.internal

import kotlin.reflect.KClass

internal typealias Provider<T> = () -> T

internal data class DiKey(
    val clazz: KClass<*>,
    val qualifier: String? = null
)