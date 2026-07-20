//
//  PRMSearchTextField.swift
//  CoreUIKit
//
//  Created by Natariannn on 9/8/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit
import PRMFoundation

@IBDesignable
public class PRMSearchTextField: UITextField {

    /// Left image of PRMSearchTextField
    let imageViewLeft: UIImageView = {
        let imageView = UIImageView(frame: CGRect(x: 0.0, y: 0, width: Sizing.tokenSizing16, height: Sizing.tokenSizing16))
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()

    let rightStackView: UIStackView = {
        let stackView = UIStackView()
        stackView.axis = .horizontal
        stackView.spacing = Sizing.tokenSizing08
        return stackView
    }()

    let buttonClear: UIButton = {
        let button = UIButton(frame: CGRect(x: 0.0, y: 0.0, width: Sizing.tokenSizing24, height: Sizing.tokenSizing24))
        return button
    }()

    private let bundle = Bundle(for: PRMSearchTextField.self)

    let leftPadding: CGFloat = Sizing.tokenSizing08
    let rightPadding: CGFloat = Sizing.tokenSizing08
    let upperPadding: CGFloat = 0.0
    public var isSearchBankLAR2: Bool = false

    public override var placeholder: String? {
        didSet {
            updatePlaceholder()
        }
    }

    /// Type of PRMSearchTextField
    /// TRUE: Navigation type, FALSE: Default type
    @IBInspectable public var isNavigation: Bool = false {
        didSet {
            updateHeight()
            updateBorder(isActive: false)
        }
    }

    public var didClearText: (() -> Void)?
    public var placeHolderFont = Typography.fontRegular16
    @objc public var borderActiveColor = Colors.tokenDark100
    @objc public var borderColor = Colors.tokenDark10
    public var borderWitdh = Sizing.tokenSizing01

    /// Theme token host cấu hình (nil = giữ default SDK).
    private var themeToken: PRMSearchBarThemeToken? { PRMThemeRegistry.shared.searchBar() }

    private let imageSearchWhite = UIImage.sdk("prm_ic_search_16_white", in: Bundle(for: PRMSearchTextField.self))
    private let imageSearchDark = UIImage.sdk("prm_ic_search_24_dark40", in: .module)
    private let imageClearDark = UIImage.sdk("prm_ic_cancel_button_16_dark40", in: .module)

    private func getKeyboardLanguage() -> String? {
        return Locale.preferredLanguages.first
    }

    public override var textInputMode: UITextInputMode? {
        if let language = getKeyboardLanguage() {
            for tim in UITextInputMode.activeInputModes {
                if let primaryLanguage = tim.primaryLanguage, primaryLanguage.contains(language) {
                    return tim
                }
            }
        }
        return super.textInputMode
    }

    public override func leftViewRect(forBounds bounds: CGRect) -> CGRect {
        var textRect = super.leftViewRect(forBounds: bounds)
        textRect.origin.x += leftPadding
        textRect.origin.y += upperPadding
        return textRect
    }

    public override func rightViewRect(forBounds bounds: CGRect) -> CGRect {
        var textRect = super.rightViewRect(forBounds: bounds)
        textRect.origin.x -= rightPadding
        textRect.origin.y += upperPadding
        return textRect
    }

    public override func textRect(forBounds bounds: CGRect) -> CGRect {
        var textRect = super.textRect(forBounds: bounds)
        textRect.origin.x = leftPadding * 2 + imageViewLeft.bounds.width
        textRect.origin.y += upperPadding
        textRect.size.width -= Sizing.tokenSizing24
        return textRect
    }

    public override func editingRect(forBounds bounds: CGRect) -> CGRect {
        var textRect = super.editingRect(forBounds: bounds)
        textRect.origin.x = leftPadding * 2 + imageViewLeft.bounds.width
        textRect.origin.y += upperPadding
        textRect.size.width -= Sizing.tokenSizing24
        return textRect
    }

    public override func draw(_ rect: CGRect) {
        updateLeftView()
        updateRightView()
        guard isFirstResponder == false else {
            return
        }
        updateBorder(isActive: false)
        updatePlaceholder()
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        updateHeight()
    }

    public override func didMoveToSuperview() {
        NotificationCenter.default.addObserver(self, selector: #selector(textFieldDidEndEditing), name: UITextField.textDidEndEditingNotification, object: self)
        NotificationCenter.default.addObserver(self, selector: #selector(textFieldDidBeginEditing), name: UITextField.textDidBeginEditingNotification, object: self)
        NotificationCenter.default.addObserver(self, selector: #selector(updateClearButtonVisibility), name: UITextField.textDidChangeNotification, object: self)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - UI
extension PRMSearchTextField {
    private func updateHeight() {
        if isNavigation {
            heightAnchor.constraint(equalToConstant: Sizing.tokenSizing32).isActive = true
        } else {
            heightAnchor.constraint(equalToConstant: Sizing.tokenSizing40).isActive = true
        }
    }

    private func updateBorder(isActive: Bool) {
        borderStyle = .none
        layer.cornerRadius = themeToken?.cornerRadius ?? Sizing.tokenSizing08
        clipsToBounds = true
        if isNavigation {
            tintColor = Colors.tokenWhite
            backgroundColor = Colors.tokenBlack.withAlphaComponent(Opacity.tokenOpacity08)
            layer.borderWidth = 0.0
        } else {
            tintColor = Colors.tokenDark100
            backgroundColor = .white
            layer.borderWidth = borderWitdh
            // Theme: khi host cấu hình borderColor → áp cho MỌI trạng thái (kể cả focus),
            // không đổi sang màu active mặc định (tokenDark100 ~ đen). Không cấu hình thì giữ hành vi cũ.
            let inactiveBorder = themeToken?.borderColor ?? borderColor
            let activeBorder = themeToken?.borderColor ?? borderActiveColor
            layer.borderColor = isActive ? activeBorder.cgColor : inactiveBorder.cgColor
        }
    }

    private func updateLeftView() {
        if isNavigation {
            leftViewMode = .always
            imageViewLeft.image = imageSearchWhite
            leftView = imageViewLeft
        } else {
            leftViewMode = .always
            // Theme: tint icon tìm kiếm nếu host cấu hình iconColor.
            if let iconColor = themeToken?.iconColor {
                imageViewLeft.image = imageSearchDark?.withRenderingMode(.alwaysTemplate)
                imageViewLeft.tintColor = iconColor
            } else {
                imageViewLeft.image = imageSearchDark
            }
            leftView = imageViewLeft
        }
    }

    private func updateRightView() {
        buttonClear.frame = CGRect(x: 0.0, y: bounds.origin.y + upperPadding, width: Sizing.tokenSizing24, height: Sizing.tokenSizing24)
        if !isSearchBankLAR2 {
            // Theme: tint icon clear đồng bộ với icon search (iconColor) nếu host cấu hình.
            if let iconColor = themeToken?.iconColor {
                buttonClear.setImage(imageClearDark?.withRenderingMode(.alwaysTemplate), for: .normal)
                buttonClear.tintColor = iconColor
            } else {
                buttonClear.setImage(imageClearDark, for: .normal)
            }
        }
        buttonClear.removeTarget(self, action: #selector(invokeButtonClear), for: .touchUpInside)
        buttonClear.addTarget(self, action: #selector(invokeButtonClear), for: .touchUpInside)
        if !rightStackView.arrangedSubviews.contains(buttonClear) {
            rightStackView.addArrangedSubview(buttonClear)
        }
        rightView = rightStackView
        // Chỉ hiện icon clear khi đã nhập từ khoá (gõ, paste...)
        removeTarget(self, action: #selector(updateClearButtonVisibility), for: .editingChanged)
        addTarget(self, action: #selector(updateClearButtonVisibility), for: .editingChanged)
        updateClearButtonVisibility()
    }

    @objc
    private func updateClearButtonVisibility() {
        let hasText = !(text?.isEmpty ?? true)
        // Bật/tắt cả vùng rightView để không chiếm chỗ & không lệch layout khi rỗng
        rightViewMode = hasText ? .always : .never
    }

    public func setButtonClear(hide: Bool) {
        if hide {
            buttonClear.setImage(UIImage(named: ""), for: .normal)
        } else if let iconColor = themeToken?.iconColor {
            buttonClear.setImage(imageClearDark?.withRenderingMode(.alwaysTemplate), for: .normal)
            buttonClear.tintColor = iconColor
        } else {
            buttonClear.setImage(imageClearDark, for: .normal)
        }
    }

    private func updatePlaceholder() {
        var mutableStringTitle = NSMutableAttributedString()
        let textPlaceholder = placeholder ?? ""
        mutableStringTitle = NSMutableAttributedString(string:textPlaceholder, attributes: [NSAttributedString.Key.font:placeHolderFont])
        // Theme: override màu placeholder + màu chữ nếu host cấu hình.
        let hintColor = themeToken?.hintTextColor ?? (isNavigation ? Colors.tokenViettelPayRed40 : Colors.tokenDark40)
        mutableStringTitle.addAttribute(NSAttributedString.Key.foregroundColor, value: hintColor,
                                        range:NSRange(location:0, length:textPlaceholder.count))
        attributedPlaceholder = mutableStringTitle
        textColor = themeToken?.textColor ?? (isNavigation ? Colors.tokenWhite : Colors.tokenDark100)
    }

    @objc
    open func textFieldDidBeginEditing() {
        updateBorder(isActive: true)
    }

    @objc
    open func textFieldDidEndEditing() {
        updateBorder(isActive: false)
    }

    @objc
    private func invokeButtonClear() {
        text = ""
        updateClearButtonVisibility()
        if let didClearText = didClearText {
            didClearText()
        } else {
            sendActions(for: UIControl.Event.editingChanged)
        }
    }

    public func displayButtonClear(_ type: Bool) {
        self.buttonClear.alpha = type ? 1 : 0
    }
}
