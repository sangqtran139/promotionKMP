//
//  SeeMoreCell.swift
//  PromotionSDK
//
//  Created by thachlh on 27/5/26.
//

import UIKit
@_implementationOnly import PRMDesignKit

final class SeeMoreCell: UITableViewCell {

    var action: (() -> Void)?

    private let titleLabel: UILabel = {
        let label = UILabel()
        label.font = Typography.fontRegular14
        label.textColor = Colors.tokenDark60
        label.numberOfLines = 1
        // Nhãn hành động: cấm mọi thứ ép co bề ngang, kể cả khi bật cỡ chữ hệ thống lớn.
        label.setContentCompressionResistancePriority(.required, for: .horizontal)
        label.setContentHuggingPriority(.required, for: .horizontal)
        return label
    }()

    private let arrowView: UIImageView = {
        let view = UIImageView()
        view.contentMode = .scaleAspectFit
        view.setContentCompressionResistancePriority(.required, for: .horizontal)
        view.setContentHuggingPriority(.required, for: .horizontal)
        return view
    }()

    /// Khe 8pt giữa chữ và mũi tên — nay là `spacing` thật của stack, không phải inset bù trừ.
    private lazy var contentStack: UIStackView = {
        let stack = UIStackView(arrangedSubviews: [titleLabel, arrowView])
        stack.axis = .horizontal
        stack.alignment = .center
        stack.spacing = 8
        stack.isUserInteractionEnabled = false   // để tap rơi xuống `tapArea`
        stack.translatesAutoresizingMaskIntoConstraints = false
        return stack
    }()

    /// Vùng bấm bọc quanh stack — thay cho `UIButton` cũ, giữ nguyên vùng chạm.
    private let tapArea: UIControl = {
        let control = UIControl()
        control.translatesAutoresizingMaskIntoConstraints = false
        return control
    }()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupUI()
    }
    
    private func setupUI() {
        self.selectionStyle = .none
        self.backgroundColor = .clear

        tapArea.addTarget(self, action: #selector(didTap), for: .touchUpInside)

        contentView.addSubview(tapArea)
        tapArea.addSubview(contentStack)

        // top = 8 để "Xem thêm" cách list voucher thoáng hơn (cộng 8px đáy item -> ~24px).
        NSLayoutConstraint.activate([
            tapArea.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            tapArea.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            tapArea.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -16),

            contentStack.leadingAnchor.constraint(equalTo: tapArea.leadingAnchor),
            contentStack.trailingAnchor.constraint(equalTo: tapArea.trailingAnchor),
            contentStack.topAnchor.constraint(equalTo: tapArea.topAnchor),
            contentStack.bottomAnchor.constraint(equalTo: tapArea.bottomAnchor)
        ])
    }

    func configure(title: String, image: UIImage?) {
        titleLabel.text = title
        arrowView.image = image
        arrowView.isHidden = (image == nil)
    }

    @objc private func didTap() {
        action?()
    }
}

