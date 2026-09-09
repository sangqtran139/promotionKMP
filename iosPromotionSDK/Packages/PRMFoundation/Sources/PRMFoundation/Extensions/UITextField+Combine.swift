//
//  UITextField+Combine.swift
//  PRMFoundation
//
//  Publisher Combine phát text của ô nhập cho tầng UI.
//

import UIKit
import Combine

public extension UITextField {
    /// Phát giá trị text hiện tại **ngay khi subscribe**, rồi mỗi lần nội dung đổi (user gõ).
    var textPublisher: AnyPublisher<String, Never> {
        NotificationCenter.default
            .publisher(for: UITextField.textDidChangeNotification, object: self)
            .compactMap { ($0.object as? UITextField)?.text }
            .prepend(text ?? "")
            .eraseToAnyPublisher()
    }
}
