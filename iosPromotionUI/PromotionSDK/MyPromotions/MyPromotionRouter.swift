//
//  MyPromotionRouter.swift
//  PromotionSDK
//
//  Created by thachlh on 6/5/26.
//

import UIKit
@_implementationOnly import PromotionKit

final class MyPromotionRouter: BaseRouter<MyPromotionViewController> {
    
    func routeToSearch(customerId: String, token: String?) {
        let vc = SearchMyPromotionBuilder.build(with: .init(customerId: customerId, token: token), navigator: navigator)
        navigator?.pushViewController(vc, animated: true)
    }
    
    func routeToDetail(promotion: VoucherItem, customerId: String, token: String?) {
        // Cờ VOUCHER_DETAIL TẮT → hiện popup PRM_MOB_021, không mở màn chi tiết.
        guard canRouteToDetail() else { return }
        let vc = PromotionDetailBuilder.build(
            with: .init(promotion: PromotionCardSeed(voucher: promotion), customerId: customerId, token: token),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
