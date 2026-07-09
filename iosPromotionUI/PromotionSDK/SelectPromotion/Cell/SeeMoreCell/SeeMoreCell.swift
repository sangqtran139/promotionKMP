//
//  SeeMoreCell.swift
//  PromotionSDK
//
//  Created by thachlh on 27/5/26.
//

import UIKit
@_implementationOnly import CoreUI

final class SeeMoreCell: UITableViewCell {
    let button = UIButton(type: .custom)
    var action: (() -> Void)?
    
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
        
        button.setTitleColor(Colors.tokenDark60, for: .normal)
        button.titleLabel?.font = Typography.fontRegular14
        
        button.semanticContentAttribute = .forceRightToLeft
        button.imageEdgeInsets = UIEdgeInsets(top: 0, left: 8, bottom: 0, right: 0)
        
        button.addTarget(self, action: #selector(didTap), for: .touchUpInside)
        
        contentView.addSubview(button)
        button.translatesAutoresizingMaskIntoConstraints = false
        // top = 8 để "Xem thêm" cách list voucher thoáng hơn (cộng 8px đáy item -> ~24px).
        NSLayoutConstraint.activate([
            button.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            button.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
            button.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -16)
        ])
    }
    
    @objc private func didTap() {
        action?()
    }
}

