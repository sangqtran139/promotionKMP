//
//  SearchMyPromotionRouter.swift
//  PromotionSDK
//
//  Created by thachlh on 11/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class SearchMyPromotionRouter: PRMBaseRouter<SearchMyPromotionViewController> {
    
    func routeToDetail(promotion: VoucherItem) {
        // Cờ VOUCHER_DETAIL TẮT → hiện toast PRM_MOB_021, không mở màn chi tiết.
        guard canOpenVoucherDetail() else { return }
        // token màn chi tiết tự đọc từ PromotionRequestContextProvider (đối xứng Android).
        let vc = PromotionDetailBuilder.build(
            with: .init(promotion: PRMPromotionCardSeed(voucher: promotion)),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
