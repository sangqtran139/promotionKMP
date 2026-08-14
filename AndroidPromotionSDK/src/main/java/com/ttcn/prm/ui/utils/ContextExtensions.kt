package com.ttcn.prm.ui.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * `PRMEndowView` là custom View đặt thẳng vào layout host (Activity hoặc Fragment) — không có
 * `context` nào khác ngoài chuỗi `ContextWrapper` để tự tìm ra [FragmentActivity] mà tự điều hướng
 * (xem `PromotionSDK.openChoosePromotion`). Themed context (`ContextThemeWrapper`, dialog...) bọc
 * ngoài activity thật nên phải đi lên hết chuỗi `baseContext`, không dừng ở lớp ngoài cùng.
 */
internal tailrec fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
