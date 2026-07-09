package com.ttcn.promotionsdk.core.util

/**
 * Khoá reentrant tối thiểu để `ComponentRegistry` giữ được double-checked locking sau khi bỏ
 * `ConcurrentHashMap` + `synchronized` (JVM-only). Android dùng `ReentrantLock`, iOS dùng
 * `NSRecursiveLock`.
 */
internal expect class SdkLock() {
    fun lock()
    fun unlock()
}

internal inline fun <T> SdkLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
