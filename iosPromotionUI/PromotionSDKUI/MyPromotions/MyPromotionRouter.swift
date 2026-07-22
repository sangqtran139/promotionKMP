//
//  MyPromotionRouter.swift
//  PromotionSDK
//
//  Created by thachlh on 6/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class MyPromotionRouter: PRMBaseRouter<MyPromotionViewController> {
    
    func routeToSearch() {
        let vc = SearchMyPromotionBuilder.build(with: .init(), navigator: navigator)
        navigator?.pushViewController(vc, animated: true)
    }

    func routeToDetail(promotion: VoucherItem) {
        // Cờ VOUCHER_DETAIL TẮT → hiện popup PRM_MOB_021, không mở màn chi tiết.
        guard canOpenVoucherDetail() else { return }
        // customerId/token màn chi tiết tự đọc từ PromotionRequestContextProvider (đối xứng Android).
        let vc = PromotionDetailBuilder.build(
            with: .init(promotion: PRMPromotionCardSeed(voucher: promotion)),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
