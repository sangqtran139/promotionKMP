//
//  DemoPromotionCallback.swift
//  PromotionSDKDemo
//
//  Callback SDK là 1-1 (một object nhận 6 sự kiện). App demo có nhiều màn muốn nghe cùng lúc, nên
//  giữ MỘT object callback dùng chung với các closure gán được — màn nào đang hiện thì gán closure
//  của mình vào. Đây là **tiện ích demo ~30 dòng**, KHÔNG phải wrapper anti-corruption:
//  mọi lời gọi SDK khác (initialize / updateOrderInfo / openMyPromotion / api) đều gọi THẲNG PromotionSDK.
//
//  Host thật chỉ cần implement `PromotionSDKCallback` đúng những sự kiện mình quan tâm rồi truyền
//  vào `PromotionSDK.initialize(callback:)` — không cần lớp fan-out này nếu chỉ có một nơi nghe.
//

import Foundation
import PRM

final class DemoPromotionCallback: PromotionSDKCallback {

    static let shared = DemoPromotionCallback()
    private init() {}

    var onApplied: ((String) -> Void)?
    var onCleared: (() -> Void)?
    var onCountChanged: ((Int) -> Void)?
    var onService: ((PromotionServiceSelection) -> Void)?

    // Protocol có default rỗng → chỉ hiện thực đúng sự kiện demo cần.
    func onVoucherApplied(voucherId: String) { onApplied?(voucherId) }
    func onVoucherCleared() { onCleared?() }
    func onVoucherCountChanged(count: Int) { onCountChanged?(count) }
    func onServiceSelected(selection: PromotionServiceSelection) { onService?(selection) }
}
