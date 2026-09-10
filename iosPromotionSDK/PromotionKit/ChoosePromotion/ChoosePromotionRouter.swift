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
        // `returnVoucherOnApply: true` → nút bên đó là "Áp dụng", bấm thì gọi `onVoucherApplied` rồi
        // tự pop về đây với voucher đã tick (TLNV MOB_002 control #5). Đối ứng Android:
        // `openPromotionDetail(voucherId, returnVoucherOnApply = true)` + `setFragmentResult(RESULT_APPLY_VOUCHER)`.
        let vc = PromotionDetailBuilder.build(
            with: .init(
                promotion: PRMPromotionCardSeed(offer: promotion),
                returnVoucherOnApply: true,
                onVoucherApplied: { [weak self] _ in
                    // Tick theo **id của offer trong danh sách này** (`EligibleOffer.id`), KHÔNG phải
                    // `VoucherDetail.voucherId` của response chi tiết: nhóm "Ưu đãi khác" là campaign
                    // chưa sở hữu nên `id` chính là `campaignId`, còn detail API trả `voucherId` khác
                    // (hoặc rỗng) → `SetPreSelected` không khớp item nào, không tick được gì.
                    // `selectedIds` của store so với `ChooseOffer.source.id`. Đối ứng Android:
                    // `setFragmentResult` mang đúng id đã dùng để mở màn.
                    self?.viewController?.selectVoucherFromDetail(promotion.id)
                }
            ),
            navigator: navigator
        )
        navigator?.pushViewController(vc, animated: true)
    }
}
