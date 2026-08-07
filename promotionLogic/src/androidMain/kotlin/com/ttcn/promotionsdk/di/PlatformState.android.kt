package com.ttcn.promotionsdk.di

import com.ttcn.promotionsdk.data.local.AndroidContextHolder

internal actual fun clearPlatformState() {
    AndroidContextHolder.clear()
}
