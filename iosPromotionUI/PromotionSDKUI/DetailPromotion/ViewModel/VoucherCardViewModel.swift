//
//  VoucherCardViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//


import Foundation

struct VoucherCardViewModel {
    let title: String
    let description: String
    /// URL logo merchant từ API (có thể nil/rỗng).
    let logoURL: String?
    let date: String
}
