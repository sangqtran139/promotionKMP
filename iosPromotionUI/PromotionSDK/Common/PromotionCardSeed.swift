//
//  PromotionCardSeed.swift
//  VDSPromotionUI
//
//  Dữ liệu tối thiểu để màn Chi tiết vẽ ngay card, trước khi API detail trả về.
//
//  **Đây là model trình bày, không phải domain model.** Nó tồn tại vì hai luồng mở màn Chi tiết
//  bằng hai kiểu khác nhau của lõi: "Ưu đãi của tôi" đưa `VoucherItem`, còn "Chọn ưu đãi"
//  (checkout) đưa `EligibleOffer`. Bản cũ gộp cả hai vào `PromotionModel` — một domain model
//  Swift trùng lặp với Kotlin. Nay chỉ giữ đúng phần mà cái card cần.
//

import Foundation
@_implementationOnly import PromotionKit

struct PromotionCardSeed {
    /// `voucherId` với voucher đã sở hữu; `voucherId ?? campaignId` với offer ở luồng checkout.
    let id: String
    /// Hiện ở label nhỏ (12pt).
    let merchantName: String
    /// Hiện ở label lớn (16pt) — tên ưu đãi.
    let name: String
    let logo: String?
    let expirationDate: String?
    let status: String?
    let displayStatusLabel: String?
    let applicableProducts: [ApplicableProduct]

    init(voucher: VoucherItem) {
        self.id = voucher.voucherId
        self.merchantName = voucher.merchantName ?? ""
        self.name = voucher.title ?? ""
        self.logo = voucher.logo
        self.expirationDate = voucher.expirationDate
        self.status = voucher.status
        self.displayStatusLabel = voucher.displayStatusLabel
        self.applicableProducts = voucher.applicableProducts
    }

    /// Seed tối thiểu khi host chỉ có `voucherId` (`openPromotionDetail`): card trống + shimmer
    /// cho tới khi màn Chi tiết fetch xong.
    init(voucherId: String) {
        self.id = voucherId
        self.merchantName = ""
        self.name = ""
        self.logo = nil
        self.expirationDate = nil
        self.status = nil
        self.displayStatusLabel = nil
        self.applicableProducts = []
    }

    init(offer: EligibleOffer) {
        self.id = offer.id
        // Eligible API không trả merchant riêng — giữ như bản cũ.
        self.merchantName = ""
        self.name = offer.campaignName ?? ""
        self.logo = nil
        self.expirationDate = offer.expireDate
        // `usable` là nguồn sự thật ở luồng checkout; map về status để dùng chung `displayState()`.
        self.status = offer.usable ? "ACTIVE" : "INELIGIBLE"
        self.displayStatusLabel = nil
        self.applicableProducts = []
    }
}
