//
//  VoucherCardViewModel.swift
//  PromotionSDK
//
//  Created by thachlh on 14/5/26.
//


import UIKit

struct VoucherCardViewModel {
    let title: String
    let description: String
    /// URL logo merchant từ API (có thể nil/rỗng).
    let logoURL: String?
    let date: String
    /// Cam khi voucher sắp hết hạn ("HSD còn X ngày"), nil = màu mặc định. Rule chung với màn danh sách.
    let dateColor: UIColor?
}
