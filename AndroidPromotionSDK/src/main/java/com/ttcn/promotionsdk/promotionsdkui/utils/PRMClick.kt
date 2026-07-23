package com.ttcn.promotionsdk.promotionsdkui.utils

import android.os.SystemClock

object PRMClick {
    private var lastTime: Long = 0
    private var lastManualTime: Long = 0

    fun check(interval: Long = 500): Boolean {
        if (interval == 500L) {
            if (SystemClock.elapsedRealtime() - lastTime >= interval) {
                lastTime = SystemClock.elapsedRealtime()
                return true
            }
        } else {
            if (SystemClock.elapsedRealtime() - lastManualTime >= interval) {
                lastManualTime = SystemClock.elapsedRealtime()
                return true
            }
        }
        return false
    }
}