package com.ttcn.promotionsdk.core.util

import java.util.concurrent.locks.ReentrantLock

internal actual class SdkLock actual constructor() {
    private val delegate = ReentrantLock()

    actual fun lock() = delegate.lock()
    actual fun unlock() = delegate.unlock()
}
