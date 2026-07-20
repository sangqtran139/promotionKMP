//
//  OfferSearchFilter.swift
//  PRMPromotionUI
//
//  Lọc danh sách ưu đãi theo từ khoá, **trong bộ nhớ**, cho ô tìm kiếm của màn "Chọn ưu đãi".
//
//  Thay cho `SearchPromotionUseCase` của gói PromotionLogic (Swift) cũ. Nó chưa từng gọi API —
//  chỉ filter mảng đã tải — nên không phải use case, và không thuộc lõi Kotlin.
//

import Foundation
@_implementationOnly import PRMKotlinBridge

enum OfferSearchFilter {

    static func search(query: String, in offers: [EligibleOffer]) -> [EligibleOffer] {
        let keyword = normalize(query)
        guard !keyword.isEmpty else { return offers }
        return offers.filter { normalize($0.campaignName ?? "").contains(keyword) }
    }

    /// Bỏ dấu và hạ chữ thường để "uu dai" khớp "Ưu đãi".
    private static func normalize(_ text: String) -> String {
        text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "vi_VN"))
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
