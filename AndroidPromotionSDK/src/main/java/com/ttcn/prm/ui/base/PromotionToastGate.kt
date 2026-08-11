package com.ttcn.prm.ui.base

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.ttcn.prm.R

/**
 * Cổng bật/tắt **toàn bộ** toast lỗi nghiệp vụ của SDK — gom một chỗ để dễ đổi.
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
     * Ngoại lệ có chủ đích: các thông báo lỗi khác còn state thay thế (empty/shimmer/list cũ) nên tắt
     * đi vẫn hiểu được; còn ở đây user bấm mà màn không mở, im lặng thì thành "app đơ". Gom vào gate để
     * mọi cửa vào (`PRMBaseFragment.openPromotionDetail`, `PromotionSDK.openMyPromotion`,
     * `PromotionSDK.openPromotionDetail`) dùng chung một quyết định.
     *
     * Đối ứng `PromotionToast.showAlways(_:in:)` bên iOS.
     */
    fun showFeatureDisabled(context: Context, fragmentManager: FragmentManager) {
        showAlways(context, fragmentManager, context.getString(R.string.prm_feature_disabled))
    }

    /**
     * Thông báo **LUÔN hiện**, không qua [isEnabled]. Chỉ dùng cho thông báo mà im lặng thì user
     * không hiểu chuyện gì (tính năng bị chặn, lỗi khi vừa chủ động bấm) — không dùng cho lỗi nghiệp
     * vụ thường. Hiện bằng [PRMBaseConfirmDialog] dạng 1 nút (không truyền `buttonNegative`) thay vì
     * Toast — Toast dễ bị hệ thống/OEM chặn hoặc trôi qua quá nhanh, dialog đảm bảo user thấy được.
     *
     * Đối ứng `PromotionToast.showAlways(_:in:)` bên iOS.
     */
    fun showAlways(context: Context, fragmentManager: FragmentManager, message: CharSequence) {
        PRMBaseConfirmDialog.newInstance(
            title = context.getString(R.string.prm_notification_title),
            content = message,
            buttonPositive = context.getString(R.string.prm_notification_button_close),
        ).show(fragmentManager, PRMBaseConfirmDialog::class.java.simpleName)
    }
}
