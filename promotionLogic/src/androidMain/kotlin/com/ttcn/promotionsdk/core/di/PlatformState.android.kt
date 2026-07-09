package com.ttcn.promotionsdk.core.di

import com.ttcn.promotionsdk.core.data.local.AndroidContextHolder

internal actual fun clearPlatformState() {
    AndroidContextHolder.clear()
}
