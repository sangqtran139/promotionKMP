//
//  ChoosePromotionRouter.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//

import UIKit
@_implementationOnly import PromotionKit

final class ChoosePromotionRouter: BaseRouter<ChoosePromotionViewController> {
    
    func routeToDetail(promotion: EligibleOffer, customerId: String, token: String?) {
        // Cờ VOUCHER_DETAIL TẮT → hiện popup PRM_MOB_021, không mở màn chi tiết.
        guard canRouteToDetail() else { return }
        let vc = PromotionDetailBuilder.build(
            with: .init(promotion: PromotionCardSeed(offer: promotion), customerId: customerId, token: token),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
