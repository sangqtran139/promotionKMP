//
//  PromotionFeature.swift
//  VDSPromotionUI
//
//  Thay cho `PromotionFeature` của gói CoreNetwork cũ. Tên cờ là hằng số của lõi Kotlin,
//  dùng chung với Android — không định nghĩa lại chuỗi ở đây.
//

import Foundation
@_implementationOnly import PromotionKit

enum PromotionFeature {
    case enableAll
    case voucherList
    case voucherDetail
    case voucherSelection
    case voucherApply
    case voucherRedeem

    var flagName: String {
        let flags = PromotionFeatureFlag.shared
        switch self {
        case .enableAll:        return flags.ENABLE_ALL
        case .voucherList:      return flags.VOUCHER_LIST
        case .voucherDetail:    return flags.VOUCHER_DETAIL
        case .voucherSelection: return flags.VOUCHER_SELECTION
        case .voucherApply:     return flags.VOUCHER_APPLY
        case .voucherRedeem:    return flags.VOUCHER_REDEEM
        }
    }
}
