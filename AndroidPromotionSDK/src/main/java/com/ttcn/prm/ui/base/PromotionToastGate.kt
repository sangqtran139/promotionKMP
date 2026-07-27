package com.ttcn.prm.ui.base

/**
 * Cổng bật/tắt **toàn bộ** toast của SDK — gom một chỗ để dễ đổi.
 *
 * Mặc định **TẮT** ([isEnabled] = false): SDK vẫn **bắt lỗi như cũ** (effect `ShowError` vẫn phát,
 * ViewModel vẫn `ConsumeError`), chỉ **không hiển thị** toast. Đổi thành `true` để bật lại.
 *
 * Đối ứng `PromotionToast.isEnabled` bên iOS — đổi một bên thì đổi bên kia.
 */
internal object PromotionToastGate {
    var isEnabled: Boolean = false
}
