//
//  PRMConfirmationDialog.swift
//  PRMDesignKit
//
//  Confirmation Dialog dùng chung (mobile) — dạng Lỗi: header "Thông báo" + nội dung + 1 nút "Đóng".
//  Dùng cho lỗi nghiệp vụ (thay toast) theo PRM_KBNV_MOB_000_Danh mục dùng chung #6.
//  UIKit thuần, không phụ thuộc RxSwift.
//

import UIKit

public final class PRMConfirmationDialog {

    /// Hiển thị dialog dạng **Lỗi**: tiêu đề (mặc định "Thông báo"), nội dung, 1 nút "Đóng".
    /// - Parameters:
    ///   - message: nội dung lỗi nghiệp vụ hiển thị cho user.
    ///   - title: tiêu đề header (mặc định "Thông báo").
    ///   - closeTitle: nhãn nút đóng (mặc định "Đóng").
    ///   - view: view chứa dialog (thường là `viewController.view`).
    ///   - onClose: callback khi user bấm "Đóng".
    public static func showError(
        _ message: String,
        title: String = "Thông báo",
        closeTitle: String = "Đóng",
        in view: UIView,
        onClose: (() -> Void)? = nil
    ) {
        let overlay = OverlayView()
        overlay.translatesAutoresizingMaskIntoConstraints = false
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.4)
        overlay.alpha = 0

        let card = UIView()
        card.translatesAutoresizingMaskIntoConstraints = false
        card.backgroundColor = Colors.tokenWhite
        card.layer.cornerRadius = 16
        card.clipsToBounds = true

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = Typography.fontBold16
        titleLabel.textColor = Colors.tokenDark100
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 0

        let messageLabel = UILabel()
        messageLabel.text = message
        messageLabel.font = Typography.fontRegular14
        messageLabel.textColor = Colors.tokenDark60
        messageLabel.textAlignment = .center
        messageLabel.numberOfLines = 0

        let divider = UIView()
        divider.backgroundColor = Colors.tokenDark05

        let closeButton = UIButton(type: .system)
        closeButton.setTitle(closeTitle, for: .normal)
        closeButton.titleLabel?.font = Typography.fontMedium16
        closeButton.setTitleColor(Colors.tokenViettelPayRed100, for: .normal)

        [titleLabel, messageLabel, divider, closeButton].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
            card.addSubview($0)
        }
        overlay.addSubview(card)
        view.addSubview(overlay)

        NSLayoutConstraint.activate([
            overlay.topAnchor.constraint(equalTo: view.topAnchor),
            overlay.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            overlay.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            overlay.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            card.centerXAnchor.constraint(equalTo: overlay.centerXAnchor),
            card.centerYAnchor.constraint(equalTo: overlay.centerYAnchor),
            card.leadingAnchor.constraint(greaterThanOrEqualTo: overlay.leadingAnchor, constant: 40),
            card.trailingAnchor.constraint(lessThanOrEqualTo: overlay.trailingAnchor, constant: -40),
            card.widthAnchor.constraint(lessThanOrEqualToConstant: 320),

            titleLabel.topAnchor.constraint(equalTo: card.topAnchor, constant: 20),
            titleLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            titleLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            messageLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8),
            messageLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            messageLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),

            divider.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 20),
            divider.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            divider.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            divider.heightAnchor.constraint(equalToConstant: 1),

            closeButton.topAnchor.constraint(equalTo: divider.bottomAnchor),
            closeButton.leadingAnchor.constraint(equalTo: card.leadingAnchor),
            closeButton.trailingAnchor.constraint(equalTo: card.trailingAnchor),
            closeButton.bottomAnchor.constraint(equalTo: card.bottomAnchor),
            closeButton.heightAnchor.constraint(equalToConstant: 48)
        ])

        overlay.onClose = { [weak overlay] in
            UIView.animate(withDuration: 0.2, animations: { overlay?.alpha = 0 }, completion: { _ in
                overlay?.removeFromSuperview()
                onClose?()
            })
        }
        closeButton.addTarget(overlay, action: #selector(OverlayView.handleClose), for: .touchUpInside)

        UIView.animate(withDuration: 0.2) { overlay.alpha = 1 }
    }

    /// Overlay giữ closure đóng — tách class để `#selector` hoạt động (target là NSObject).
    private final class OverlayView: UIView {
        var onClose: (() -> Void)?
        @objc func handleClose() { onClose?() }
    }
}
