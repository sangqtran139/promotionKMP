package com.ttcn.promotionsdk.common

import platform.Foundation.NSRecursiveLock

internal actual class SdkLock actual constructor() {
    private val delegate = NSRecursiveLock()

    actual fun lock() = delegate.lock()
    actual fun unlock() = delegate.unlock()
}
