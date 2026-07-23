//
//  UnderlinedSegmentControlItem.swift
//  PRMPromotionUI
//
//  Created by thachlh on 13/5/26.
//

import PRMDesignKit
import UIKit
import PRMFoundation

final class UnderlinedSegmentControlItem: PRMTapableView {
    //MARK: - Properties
    public var id = UUID()
    public var title: String = "" {
        didSet {
            self.titleLabel.text = self.title
        }
    }
    
    public var isFocus: Bool = false {
        didSet {
            self.updateStateFocused()
        }
    }
    
    private var titleLabel: UILabel!
    private var lineView: PRMBaseView!

    /// Theme token host cấu hình (nil = giữ default SDK).
    private var themeToken: PRMTabUnderlineThemeToken? { PRMThemeRegistry.shared.tabUnderline() }
    
    //MARK: - Init
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.config()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.config()
    }
    
    //MARK: - Config
    private func config() {
        self.configTitleLable()
        self.configLineView()
        
    }
    
    private func configTitleLable() {
        self.titleLabel = UILabel()
        self.titleLabel.text = self.title
        self.titleLabel.font = Typography.fontBold14
        self.titleLabel.numberOfLines = 0
        self.titleLabel.textColor = themeToken?.activeTextColor ?? Colors.tokenDark100
        self.addSubview(self.titleLabel)
        self.titleLabel.makeAnchor { maker in
            maker.top(equalTo: self.topAnchor)
                .centerX(equalTo: self.centerXAnchor)
        }
    }
    
    private func configLineView() {
        self.lineView = PRMBaseView()
        self.lineView.cornerRadius = 1.5
        self.lineView.backgroundColor = themeToken?.indicatorColor ?? Colors.tokenRed100
        self.addSubview(self.lineView)
        self.lineView.makeAnchor { maker in
            maker.top(equalTo: self.titleLabel.bottomAnchor, constant: 8)
                .height(equalTo: 3)
                .bottom(equalTo: self.bottomAnchor)
                .leading(equalTo: self.leadingAnchor)
                .trailing(equalTo: self.trailingAnchor)
        }
    }
    
    //MARK: - Helper
    private func updateStateFocused() {
        self.titleLabel.font = self.isFocus ? Typography.fontBold14 : Typography.fontRegular14
        // Theme: override màu chữ active/inactive + màu indicator (null-safe).
        let activeText = themeToken?.activeTextColor ?? Colors.tokenDark100
        let inactiveText = themeToken?.inactiveTextColor ?? Colors.tokenDark60
        self.titleLabel.textColor = self.isFocus ? activeText : inactiveText

        let indicator = themeToken?.indicatorColor ?? Colors.tokenRed100
        self.lineView.backgroundColor = self.isFocus ? indicator : Colors.tokenDark20
    }
}
