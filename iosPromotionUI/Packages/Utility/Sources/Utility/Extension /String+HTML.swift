//
//  String+HTML.swift
//  Utility
//

import UIKit

public extension String {
    /// Convert chuỗi HTML sang NSAttributedString.
    /// Áp `font`/`color` cho toàn bộ nội dung nhưng vẫn giữ các trait bold/italic từ HTML.
    /// Lưu ý: phải gọi trên main thread (yêu cầu của NSAttributedString HTML parser).
    func htmlToAttributedString(font: UIFont, color: UIColor) -> NSAttributedString? {
        guard let data = self.data(using: .utf8) else { return nil }

        let options: [NSAttributedString.DocumentReadingOptionKey: Any] = [
            .documentType: NSAttributedString.DocumentType.html,
            .characterEncoding: String.Encoding.utf8.rawValue
        ]

        guard let attributed = try? NSMutableAttributedString(
            data: data,
            options: options,
            documentAttributes: nil
        ) else {
            return nil
        }

        let fullRange = NSRange(location: 0, length: attributed.length)
        attributed.addAttribute(.foregroundColor, value: color, range: fullRange)

        // Thay font family/size theo design nhưng giữ bold/italic mà HTML đã gắn.
        attributed.enumerateAttribute(.font, in: fullRange, options: []) { value, range, _ in
            let traits = (value as? UIFont)?.fontDescriptor.symbolicTraits ?? []
            if let descriptor = font.fontDescriptor.withSymbolicTraits(traits) {
                attributed.addAttribute(.font, value: UIFont(descriptor: descriptor, size: font.pointSize), range: range)
            } else {
                attributed.addAttribute(.font, value: font, range: range)
            }
        }

        return attributed
    }
}
