//
//  PRMButton.swift
//  CoreUIKit
//
//  Created by Natariannn on 9/3/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit
import PRMFoundation

public enum PRMButtonSize: Int {
    case small
    case medium
    case large
}

@IBDesignable
public class PRMButton: UIButton {
    
    private var shadowView: PRMShadowView?
    private var gradientView: PRMPassThroughView?
    
    private var imageViewLoading: UIImageView?
    
    private var gradientLayer: CAGradientLayer = CAGradientLayer()
    
    let bundle = Bundle(for: PRMButton.self)
    
    /// Type of PRMButton
    /// TRUE: White button, FALSE: Primary button
    @IBInspectable public var isWhite: Bool = false {
        didSet {
            commonInit()
        }
    }
    
    /// Type of PRMButton
    /// TRUE: Outline type, FALSE: Gradient type
    @IBInspectable public var isOutline: Bool = false {
        didSet {
            commonInit()
        }
    }
    
    /// Size for PRMButton, default is 2
    /// If size is different from PRMButtonSize, default is medium size
    @IBInspectable public var size: Int = 2 {
        didSet {
            commonInit()
        }
    }
    
    @IBInspectable public var isLoading: Bool = false {
        didSet {
            setupImageViewLoading()
        }
    }
    
    lazy var leftImageView: UIImageView = UIImageView()
    public var leftTitleImage: UIImage?
    
    /// Height from each size of PRMButton
    private let smallHeight = Sizing.tokenSizing24
    private let mediumHeight = Sizing.tokenSizing32
    private let largeHeight = Sizing.tokenSizing48
    
    private var maskRadius: CGFloat = 0.0

    private let imageLoading = UIImage.sdk("prm_ic_change_32_white", in: .main)

    /// Theme token host cấu hình (nil = giữ default SDK).
    private var themeToken: PRMButtonThemeToken? { PRMThemeRegistry.shared.button() }
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        commonInit(isFirstTime: true)
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        commonInit(isFirstTime: true)
    }
    
    deinit {
        removeNotification()
    }
    
    override public func draw(_ rect: CGRect) {
        super.draw(rect)
        guard let gradientView = gradientView else {
            return
        }
        // Theme: nếu host cấu hình backgroundColor → dùng màu đơn sắc, ngược lại giữ gradient mặc định.
        if let bg = themeToken?.backgroundColor {
            gradientLayer.colors = [bg.cgColor, bg.cgColor]
        } else {
            gradientLayer.colors = [Colors.tokenViettelPayRed80.cgColor, Colors.tokenViettelPayRed100.cgColor]
        }
        gradientLayer.frame = gradientView.bounds
        gradientLayer.startPoint = CGPoint(x: 0.0, y: 1.0)
        gradientLayer.endPoint = CGPoint(x: 1.0, y: 1.0)
        gradientLayer.locations = [0, 1]
    }
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        loadingState()
        if let leftTitleImage = leftTitleImage {
            self.setImage(with: leftTitleImage)
        }
    }
    
    public override var isSelected: Bool {
        didSet {
            if oldValue != isSelected {
                updateAppearance()
            }
        }
    }
    
    public override var isHighlighted: Bool {
        didSet {
            if oldValue != isHighlighted {
                updateAppearance()
            }
        }
    }
    
    public override var isEnabled: Bool {
        didSet {
            if oldValue != isEnabled {
                updateAppearance()
            }
        }
    }
}

extension PRMButton {
    private func commonInit(isFirstTime: Bool = false) {
        setupButton(isFirstTime: isFirstTime)
        setupGradientAndShadow()
        setupImageViewLoading(isFirstTime: isFirstTime)
        updateAppearance()
        registerNotification()
    }
    
    private func registerNotification() {
        NotificationCenter.default.addObserver(self, selector: #selector(didBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
    }
    
    private func removeNotification() {
        NotificationCenter.default.removeObserver(self)
    }
    
    private func setupButton(isFirstTime: Bool = false) {
        if isWhite {
            setTitleColor(Colors.tokenDark100, for: .normal)
        } else {
            if isOutline {
                setTitleColor(Colors.tokenViettelPayRed100, for: .normal)
            } else {
                setTitleColor(Colors.tokenWhite, for: .normal)
            }
        }
        var height = smallHeight
        switch size {
            
        case PRMButtonSize.small.rawValue:
            contentEdgeInsets = UIEdgeInsets(top: 0, left: Spacing.tokenSpacing08, bottom: 0, right: Spacing.tokenSpacing08)
            titleLabel?.font = Typography.fontMedium12
            height = smallHeight
        case PRMButtonSize.medium.rawValue:
            contentEdgeInsets = UIEdgeInsets(top: 0, left: Spacing.tokenSpacing16, bottom: 0, right: Spacing.tokenSpacing16)
            titleLabel?.font = Typography.fontMedium14
            height = mediumHeight
        case PRMButtonSize.large.rawValue:
            contentEdgeInsets = UIEdgeInsets(top: 0, left: Spacing.tokenSpacing24, bottom: 0, right: Spacing.tokenSpacing24)
            titleLabel?.font = Typography.fontMedium18
            height = largeHeight
        default:
            contentEdgeInsets = UIEdgeInsets(top: 0, left: Spacing.tokenSpacing24, bottom: 0, right: Spacing.tokenSpacing24)
            titleLabel?.font = Typography.fontMedium18
            height = largeHeight
        }
        updateHeightConstraint(height)
        maskRadius = height / 2
        // Theme: override bo góc + màu chữ nếu host cấu hình (null-safe — bỏ qua khi nil).
        if let radius = themeToken?.cornerRadius {
            maskRadius = radius
        }
        if let textColor = themeToken?.textColor {
            setTitleColor(textColor, for: .normal)
        }
    }
    
    private func setupGradientAndShadow() {
        if isWhite {
            backgroundColor = .white
            layer.cornerRadius = maskRadius
            layer.borderColor = UIColor.clear.cgColor
            layer.borderWidth = 0.0
        } else {
            if isOutline {
                backgroundColor = .white
                layer.cornerRadius = maskRadius
                layer.borderColor = Colors.tokenViettelPayRed100.cgColor
                layer.borderWidth = Sizing.tokenSizing01
                clipsToBounds = true
                if shadowView != nil {
                    shadowView!.removeFromSuperview()
                }
                if gradientView != nil {
                    gradientView!.removeFromSuperview()
                }
            } else {
                backgroundColor = .clear
                if shadowView == nil {
                    shadowView = self.getShadowView()
                }
                addSubview(shadowView!)
                shadowView!.pinEdges(to: self)
                shadowView!.cornerRadius = maskRadius
                // Theme: override màu đổ bóng nếu host cấu hình.
                if let shadowColor = themeToken?.shadowColor {
                    var shadow = shadowView!.shadow
                    shadow.color = shadowColor
                    shadowView!.shadow = shadow
                }
                
                if gradientView == nil {
                    gradientView = PRMPassThroughView()
                }
                addSubview(gradientView!)
                gradientView!.makeAnchor { make in make.edges(to: self) }
                gradientView!.backgroundColor = .white
                gradientView!.layer.cornerRadius = maskRadius
                sendSubviewToBack(gradientView!)
                sendSubviewToBack(shadowView!)
                
                gradientView!.clipsToBounds = true
                gradientView!.layer.insertSublayer(gradientLayer, at: 0)
            }
        }
    }
    
    private func setupImageViewLoading(isFirstTime: Bool = false) {
        if !isLoading {
            isUserInteractionEnabled = true
            if imageViewLoading != nil {
                imageViewLoading!.removeFromSuperview()
            }
            return
        }
        if imageViewLoading == nil {
            imageViewLoading = UIImageView()
        }
        isUserInteractionEnabled = false
        addSubview(imageViewLoading!)
        imageViewLoading!.backgroundColor = .clear
        var height = Sizing.tokenSizing16
        switch size {
            
        case PRMButtonSize.small.rawValue:
            height = Sizing.tokenSizing16
            
        case PRMButtonSize.medium.rawValue:
            height = Sizing.tokenSizing24
            
        case PRMButtonSize.large.rawValue:
            height = Sizing.tokenSizing32
            
        default:
            height = Sizing.tokenSizing32
        }
        if isFirstTime {
            imageViewLoading!.makeAnchor { make in
                make.center(in: self)
                    .size(CGSize(width: height, height: height))
            }
        } else {
            imageViewLoading!.updateSizeConstraints(CGSize(width: height, height: height))
        }
        imageViewLoading!.image = imageLoading
        loadingState()
    }
    
    private func setImage(with image: UIImage) {
        self.addSubview(leftImageView)
        self.leftImageView.makeAnchor { make in
            make.centerY(equalTo: centerYAnchor)
                .centerX(equalTo: centerXAnchor, constant: -((self.titleLabel?.frame.size.width ?? 0.0) / 2))
                .size(CGSize(width: 40, height: 40))
        }
        self.leftImageView.image = image
    }
    
    private func updateAppearance() {
        if isEnabled {
            if isSelected || isHighlighted {
                highlightState()
            } else {
                enableState()
            }
        } else {
            disableState()
        }
    }
    
    
    @objc
    private func didBecomeActive() {
        loadingState()
    }
    
    private func getShadowView() -> PRMShadowView {
        switch size {
        case PRMButtonSize.large.rawValue:
            return PRMMediumButtonShadowView()
        case PRMButtonSize.medium.rawValue:
            return PRMMediumButtonShadowView()
        case PRMButtonSize.small.rawValue:
            return PRMSmallButtonShadowView()
        default:
            return PRMLargeButtonShadowView()
        }
    }
}

// MARK: - State
extension PRMButton {
    private func highlightState() {
        setupButton()
        if isWhite {
            backgroundColor = UIColor.white.withAlphaComponent(0.9)
        } else {
            if isOutline {
                layer.borderColor = Colors.tokenViettelPayRed100.withAlphaComponent(0.5).cgColor
            } else {
                guard let gradientView = gradientView else {
                    return
                }
                gradientView.alpha = 0.9
                alpha = 0.9
            }
        }
    }
    
    private func enableState() {
        setupButton()
        if isWhite {
            backgroundColor = .white
        } else {
            if isOutline {
                layer.borderColor = Colors.tokenViettelPayRed100.cgColor
                // Theme: token textColor thắng default ở state outline.
                setTitleColor(themeToken?.textColor ?? Colors.tokenViettelPayRed100, for: .normal)
            } else {
                guard let gradientView = gradientView else {
                    return
                }
                gradientView.alpha = 1.0
                alpha = 1.0
            }
        }
    }
    
    public func disableState() {
        setupButton()
        if isWhite {
            backgroundColor = UIColor.white.withAlphaComponent(0.5)
        } else {
            if isOutline {
                let color = Colors.tokenViettelPayRed100.withAlphaComponent(0.5)
                layer.borderColor = color.cgColor
                // Theme: token textColor thắng default (giữ alpha disabled).
                let titleColor = (themeToken?.textColor ?? Colors.tokenViettelPayRed100).withAlphaComponent(0.5)
                setTitleColor(titleColor, for: .normal)
            } else {
                guard let gradientView = gradientView else {
                    return
                }
                gradientView.alpha = 0.5
                alpha = 0.5
            }
        }
    }
    
    private func loadingState() {
        if isLoading {
            guard let imageViewLoading = imageViewLoading else {
                return
            }
            
            setTitleColor(.clear, for: .normal)
            
            if isWhite {
                imageViewLoading.tintColor = Colors.tokenDark100
            } else {
                if isOutline {
                    imageViewLoading.tintColor = Colors.tokenViettelPayRed100
                } else {
                    imageViewLoading.tintColor = .white
                }
            }
            
            rotateImageView()
        }
    }
    
    private func rotateImageView() {
        guard let imageViewLoading = imageViewLoading else {
            return
        }
        let rotation: CABasicAnimation = CABasicAnimation(keyPath: "transform.rotation.z")
        rotation.toValue = Double.pi * 2
        rotation.duration = 2.0
        rotation.isCumulative = true
        rotation.repeatCount = .greatestFiniteMagnitude
        imageViewLoading.layer.add(rotation, forKey: "rotationAnimation")
    }
}

// MARK: - Public function
public extension PRMButton {
    func getMaskRadius() -> CGFloat {
        return maskRadius
    }
}
