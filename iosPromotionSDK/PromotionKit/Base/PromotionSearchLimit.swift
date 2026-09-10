//
//  PromotionSearchLimit.swift
//  PromotionSDK
//
//  Giới hạn độ dài từ khoá tìm kiếm — dùng chung màn "Tìm kiếm" và "Chọn ưu đãi".
//

import UIKit

enum PromotionSearchLimit {

    /// Số ký tự tối đa của ô tìm kiếm (TLNV MOB_001 control 5.2 / MOB_004 control 2.1).
    /// Đối ứng `PROMOTION_SEARCH_MAX_LENGTH` bên `promotionLogic` — đổi thì đổi cả hai.
    static let maxKeywordLength = 255

    /// Dùng trong `textField(_:shouldChangeCharactersIn:replacementString:)`: chặn nhập khi vượt
    /// giới hạn, nhưng **không chặn xoá** và vẫn cho dán phần còn vừa chỗ (giống `LengthFilter`
    /// của Android).
    static func shouldChange(_ textField: UITextField, range: NSRange, replacement: String) -> Bool {
        let current = textField.text ?? ""
        guard let stringRange = Range(range, in: current) else { return false }
        let updated = current.replacingCharacters(in: stringRange, with: replacement)
        return updated.count <= maxKeywordLength
    }
}
