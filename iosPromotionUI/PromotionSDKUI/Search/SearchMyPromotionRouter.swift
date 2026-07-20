//
//  SearchMyPromotionRouter.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionRouter: BaseRouter<SearchMyPromotionViewController> {
    
    func routeToDetail(promotion: VoucherItem, customerId: String, token: String?) {
        // Cờ VOUCHER_DETAIL TẮT → hiện popup PRM_MOB_021, không mở màn chi tiết.
        guard canOpenVoucherDetail() else { return }
        let vc = PromotionDetailBuilder.build(
            with: .init(promotion: PromotionCardSeed(voucher: promotion), customerId: customerId, token: token),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
