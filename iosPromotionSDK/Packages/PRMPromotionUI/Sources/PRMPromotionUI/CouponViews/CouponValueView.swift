//
//  CouponValueView.swift
//  PRMDesignKit
//
//  Created by thachlh on 20/5/4.
//

import UIKit
import PRMFoundation

public class CouponValueView: UIView {
    
    // MARK: - UI Components
    
    private let backgroundView: CouponBackgroundView = {
        let view = CouponBackgroundView()
        view.cutoutOrientation = .leftRight
        view.showsDashedLine = false
        view.dashPositionRatio = 0.5
        return view
    }()
    
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.textAlignment = .center
        label.numberOfLines = 0
        return label
    }()
    
    // MARK: - Configuration
    
    public var title: String? {
        get { titleLabel.text }
        set { titleLabel.text = newValue }
    }
    
    public var attributedTitle: NSAttributedString? {
        get { titleLabel.attributedText }
        set { titleLabel.attributedText = newValue }
    }
    
    public var titleFont: UIFont? {
        get { titleLabel.font }
        set { titleLabel.font = newValue }
    }
    
    public var titleColor: UIColor? {
        get { titleLabel.textColor }
        set { titleLabel.textColor = newValue }
    }
    
    public var titleNumberOfLines: Int {
        get { titleLabel.numberOfLines }
        set { titleLabel.numberOfLines = newValue }
    }
    
    public var titleAlignment: NSTextAlignment {
        get { titleLabel.textAlignment }
        set { titleLabel.textAlignment = newValue }
    }
    
    public var couponFillColor: UIColor {
        get { backgroundView.fillColor }
        set { backgroundView.fillColor = newValue }
    }
    
    public var couponCornerRadius: CGFloat {
        get { backgroundView.cornerRadius }
        set { backgroundView.cornerRadius = newValue }
    }
    
    public var cutoutRadius: CGFloat {
        get { backgroundView.cutoutRadius }
        set { backgroundView.cutoutRadius = newValue }
    }
    
    public var showsShadow: Bool {
        get { backgroundView.showsShadow }
        set { backgroundView.showsShadow = newValue }
    }
    
    public var cutoutPositionRatio: CGFloat {
        get { backgroundView.dashPositionRatio }
        set { backgroundView.dashPositionRatio = newValue }
    }
    
    // MARK: - Init
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
        setupConstraints()
    }
    
    public required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
        setupConstraints()
    }
    
    // MARK: - Private
    
    private func setupView() {
        backgroundColor = .clear
        addSubview(backgroundView)
        backgroundView.addSubview(titleLabel)
    }
    
    private func setupConstraints() {
        backgroundView.makeAnchor { make in
            make.edges(to: self)
        }
        
        titleLabel.makeAnchor { make in
            make.center(in: self)
                .leading(greaterThanOrEqualTo: self.leadingAnchor, constant: 12)
                .trailing(lessThanOrEqualTo: self.trailingAnchor, constant: -12)
                .top(greaterThanOrEqualTo: self.topAnchor, constant: 8)
                .bottom(lessThanOrEqualTo: self.bottomAnchor, constant: -8)
        }
    }
}
