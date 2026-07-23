//
//  ChoosePromotionRouter.swift
//  PRMSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class ChoosePromotionRouter: PRMBaseRouter<ChoosePromotionViewController> {
    
    func routeToDetail(promotion: EligibleOffer) {
        // Cờ VOUCHER_DETAIL TẮT → hiện popup PRM_MOB_021, không mở màn chi tiết.
        guard canOpenVoucherDetail() else { return }
        // customerId/token màn chi tiết tự đọc từ PromotionRequestContextProvider (đối xứng Android).
        let vc = PromotionDetailBuilder.build(
            with: .init(promotion: PRMPromotionCardSeed(offer: promotion)),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
