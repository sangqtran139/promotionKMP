//
//  PRMBaseView.swift
//  PRMDesignKit
//

import UIKit

@IBDesignable
open class PRMBaseView: UIView {
    
    // MARK: - IBInspectable
    
    @IBInspectable public var cornerRadius: CGFloat = 0 {
        didSet { updateCornerRadius() }
    }
    
    @IBInspectable public var borderColor: UIColor? {
        didSet { layer.borderColor = borderColor?.cgColor }
    }
    
    @IBInspectable public var borderWidth: CGFloat = 0 {
        didSet { layer.borderWidth = borderWidth }
    }
    
    // MARK: - Corner Mask
    
    public var roundedCorners: CACornerMask = [.layerMinXMinYCorner, .layerMaxXMinYCorner,
                                                .layerMinXMaxYCorner, .layerMaxXMaxYCorner] {
        didSet { updateCornerRadius() }
    }
    
    // MARK: - Private
    
    private func updateCornerRadius() {
        layer.cornerRadius = cornerRadius
        layer.maskedCorners = roundedCorners
        layer.masksToBounds = cornerRadius > 0
    }
}

// MARK: - Corner Mask Helpers

public extension PRMBaseView {
    
    func roundTopCorners(_ radius: CGFloat? = nil) {
        roundedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        if let radius { cornerRadius = radius }
    }
    
    func roundBottomCorners(_ radius: CGFloat? = nil) {
        roundedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        if let radius { cornerRadius = radius }
    }
    
    func roundLeftCorners(_ radius: CGFloat? = nil) {
        roundedCorners = [.layerMinXMinYCorner, .layerMinXMaxYCorner]
        if let radius { cornerRadius = radius }
    }
    
    func roundRightCorners(_ radius: CGFloat? = nil) {
        roundedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        if let radius { cornerRadius = radius }
    }
    
    func roundAllCorners(_ radius: CGFloat? = nil) {
        roundedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner,
                          .layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        if let radius { cornerRadius = radius }
    }
}
