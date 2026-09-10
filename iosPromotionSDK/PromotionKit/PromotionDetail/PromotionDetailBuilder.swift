//
//  PromotionDetailBuilder.swift
//  PromotionSDK
//
//  Created by thachlh on 13/5/26.
//

import UIKit
@_implementationOnly import PRMKotlinBridge

final class PromotionDetailBuilder: PRMBaseBuilder<PromotionDetailViewController, PromotionDetailViewModel, PromotionDetailRouter, PromotionDetailBuilder.DataModel> {
    
    struct DataModel {
        /// Promotion cơ bản (từ list) để hiện card NGAY; màn tự fetch detail đầy đủ theo voucherId.
        /// token/service KHÔNG ở đây — ViewModel đọc từ PromotionRequestContextProvider
        /// của lõi (đối xứng Android).
        let promotion: PRMPromotionCardSeed

        /**
         Quyết định **nhãn nút và hành vi khi bấm** (TLNV MOB_002 control #5):
         - `false` (mặc định): "Sử dụng ngay" → chọn dịch vụ (1 dịch vụ thì đi thẳng).
         - `true`: "Áp dụng" → trả voucherId về nơi đã mở màn rồi tự pop.

         Boolean chứ không phải enum: nơi mở màn có thể là "Chọn ưu đãi" nội bộ **hoặc** màn bất kỳ
         của host qua `PromotionSDK.openPromotionDetail(...)`, nên đặt tên theo *hành vi* thay vì
         theo *màn gọi*. Đối ứng `PromotionDetailFragment.returnVoucherOnApply` bên Android.
         */
        let returnVoucherOnApply: Bool

        /**
         `true` → bấm "Áp dụng" xong SDK **không** tự pop; host tự đóng trong `onVoucherApplied`.
         Dùng khi host cần hỏi xác nhận, chạy animation riêng, hoặc đẩy thẳng sang màn khác.

         Chỉ có nghĩa khi `returnVoucherOnApply == true` — nhánh "Sử dụng ngay" không pop bao giờ.
         Đối ứng `PromotionDetailFragment.hostHandlesDismiss` bên Android.
         */
        let hostHandlesDismiss: Bool

        /// Chỉ dùng khi `returnVoucherOnApply == true`: nơi mở màn nhận lại chi tiết voucher (màn
        /// "Chọn ưu đãi" chỉ lấy `voucherId`, host dùng cả object). Router tự pop; closure lo data.
        ///
        /// Kiểu là `VoucherDetail` **domain**, không phải DTO public `PromotionVoucherDetail`: module
        /// này (`PRMPromotionUI`) nằm **dưới** `Entry/API` trong chiều phụ thuộc nên không thấy type
        /// public. `PromotionSDKImpl` map ở ranh giới — đối ứng Android (fragment cầm domain,
        /// `PromotionSDK` map).
        let onVoucherApplied: ((VoucherDetail) -> Void)?

        init(
            promotion: PRMPromotionCardSeed,
            returnVoucherOnApply: Bool = false,
            hostHandlesDismiss: Bool = false,
            onVoucherApplied: ((VoucherDetail) -> Void)? = nil
        ) {
            self.promotion = promotion
            self.returnVoucherOnApply = returnVoucherOnApply
            self.hostHandlesDismiss = hostHandlesDismiss
            self.onVoucherApplied = onVoucherApplied
        }
    }
    
    static func build(with data: DataModel, navigator: UINavigationController? = nil) -> PromotionDetailViewController {
        let builder = PromotionDetailBuilder()
        return builder.build(with: data, navigator: navigator)
    }
    
    override func createRouter() -> PromotionDetailRouter {
        return PromotionDetailRouter()
    }
    
    override func createViewModel(router: PromotionDetailRouter, data: DataModel) -> PromotionDetailViewModel {
        return PromotionDetailViewModel(router: router, data: data)
    }
    
    override func createViewController(viewModel: PromotionDetailViewModel) -> PromotionDetailViewController {
        return PromotionDetailViewController(viewModel: viewModel)
    }
}
