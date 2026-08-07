package com.ttcn.promotionsdk.di.internal

import kotlin.reflect.KClass

internal typealias Provider<T> = () -> T

internal data class DiKey(
    val clazz: KClass<*>,
    val qualifier: String? = null
)
