package com.ttcn.promotionsdk.app

import com.ttcn.prm.entry.PromotionSDKCallback
import com.ttcn.prm.entry.PromotionServiceSelection

/**
 * Callback SDK là 1-1 (một object nhận các sự kiện). App demo có nhiều màn muốn nghe cùng lúc, nên
 * giữ MỘT object callback dùng chung với các closure gán được — màn nào đang hiện thì gán closure của
 * mình vào. Đây là **tiện ích demo ~30 dòng**, KHÔNG phải wrapper anti-corruption: mọi lời gọi SDK
 * khác (initialize / updateOrderInfo / openMyPromotion / api) đều gọi THẲNG PromotionSDK.
 *
 * Soi gương `DemoPromotionCallback.swift` bên iOS — cùng 4 closure (`onApplied` / `onCleared` /
 * `onCountChanged` / `onService`) forward từ 4 event tương ứng. Sửa một bên thì sửa cả hai.
 *
 * Host thật chỉ cần implement [PromotionSDKCallback] đúng sự kiện mình quan tâm rồi truyền vào
 * `PromotionSDK.initialize(callback = ...)` — không cần lớp fan-out này nếu chỉ có một nơi nghe.
 */
object DemoPromotionCallback : PromotionSDKCallback {

    var onApplied: ((String) -> Unit)? = null
    var onCleared: (() -> Unit)? = null
    var onCountChanged: ((Int) -> Unit)? = null
    var onService: ((PromotionServiceSelection) -> Unit)? = null

    // Interface có default rỗng → chỉ override đúng sự kiện demo cần.
    override fun onVoucherApplied(voucherId: String) { onApplied?.invoke(voucherId) }
    override fun onServiceSelected(selection: PromotionServiceSelection) { onService?.invoke(selection) }
}
