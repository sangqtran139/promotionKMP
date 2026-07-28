//
//  ChoosePromotionRouter.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionRouter: PRMBaseRouter<ChoosePromotionViewController> {
    
    func routeToDetail(promotion: EligibleOffer) {
        // Cờ VOUCHER_DETAIL TẮT → hiện toast PRM_MOB_021, không mở màn chi tiết.
        guard canOpenVoucherDetail() else { return }
        // token màn chi tiết tự đọc từ PromotionRequestContextProvider (đối xứng Android).
        //
        // `entry: .checkout` → nút bên đó là "Áp dụng", bấm thì gọi `onApplyFromCheckout` rồi tự pop
        // về đây với voucher đã tick (TLNV MOB_002 control #5). Đối ứng Android: `PromotionDetailEntry`
        // + `setFragmentResult(RESULT_APPLY_VOUCHER)`.
        let vc = PromotionDetailBuilder.build(
            with: .init(
                promotion: PRMPromotionCardSeed(offer: promotion),
                entry: .checkout,
                onApplyFromCheckout: { [weak self] voucherId in
                    self?.viewController?.selectVoucherFromDetail(voucherId)
                }
            ),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
