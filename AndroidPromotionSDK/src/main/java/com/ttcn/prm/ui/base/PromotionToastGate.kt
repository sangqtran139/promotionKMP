package com.ttcn.prm.ui.base

import android.content.Context
import android.widget.Toast
import com.ttcn.prm.R

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

    /**
     * Thông báo "tính năng bị cờ chặn" (PRM_MOB_021) — **LUÔN hiện**, không đi qua [isEnabled].
     *
     * Ngoại lệ có chủ đích: các toast lỗi khác còn state thay thế (empty/shimmer/list cũ) nên tắt đi
     * vẫn hiểu được; còn ở đây user bấm mà màn không mở, im lặng thì thành "app đơ". Gom vào gate để
     * mọi cửa vào (`PRMBaseFragment.openPromotionDetail`, `PromotionSDK.openMyPromotion`,
     * `PromotionSDK.openPromotionDetail`) dùng chung một quyết định.
     *
     * Đối ứng `PromotionToast.showAlways(_:in:)` bên iOS.
     */
    fun showFeatureDisabled(context: Context) {
        showAlways(context, context.getString(R.string.prm_feature_disabled))
    }

    /**
     * Toast **LUÔN hiện**, không qua [isEnabled]. Chỉ dùng cho thông báo mà im lặng thì user không
     * hiểu chuyện gì (tính năng bị chặn, đã chọn dịch vụ) — không dùng cho lỗi nghiệp vụ thường.
     *
     * Đối ứng `PromotionToast.showAlways(_:in:)` bên iOS.
     */
    fun showAlways(context: Context, message: CharSequence) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
