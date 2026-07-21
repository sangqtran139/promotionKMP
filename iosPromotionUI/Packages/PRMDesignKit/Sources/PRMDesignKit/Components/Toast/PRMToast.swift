//
//  PRMToast.swift
//  PRMDesignKit
//
//  Toast nhẹ tự ẩn — thông báo ngắn (vd lỗi áp dụng voucher). UIKit thuần.
//

import UIKit

public final class PRMToast {

    /// Hiện toast nhẹ ở đáy safe-area của `view`, fade-in rồi tự ẩn sau `duration`.
    /// - Parameters:
    ///   - message: nội dung hiển thị.
    ///   - view: view chứa toast (thường là `viewController.view`).
    ///   - duration: thời gian giữ toast trước khi tự ẩn (giây).
    public static func show(_ message: String, in view: UIView, duration: TimeInterval = 2.0) {
        let container = UIView()
        container.backgroundColor = UIColor.black.withAlphaComponent(0.85)
        container.layer.cornerRadius = 12
        container.clipsToBounds = true
        container.alpha = 0
        container.translatesAutoresizingMaskIntoConstraints = false

        let label = UILabel()
        label.text = message
        label.textColor = .white
        label.font = .systemFont(ofSize: 14, weight: .medium)
        label.numberOfLines = 0
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false

        container.addSubview(label)
        view.addSubview(container)

        NSLayoutConstraint.activate([
            label.topAnchor.constraint(equalTo: container.topAnchor, constant: 12),
            label.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -12),
            label.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 16),
            label.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -16),

            container.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            container.leadingAnchor.constraint(greaterThanOrEqualTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            container.trailingAnchor.constraint(lessThanOrEqualTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            container.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -32)
        ])

        UIView.animate(withDuration: 0.25, animations: {
            container.alpha = 1
        }, completion: { _ in
            UIView.animate(withDuration: 0.25, delay: duration, options: [], animations: {
                container.alpha = 0
            }, completion: { _ in
                container.removeFromSuperview()
            })
        })
    }
}
