//
//  CoreShadowView.swift
//  CoreUIKit
//
//  Created by Bui Thien Thien on 9/8/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit

@IBDesignable
public class PRMShadowView: PRMPassThroughView {
    @IBInspectable public var cornerRadius: CGFloat = 0 {
        didSet {
            appearance()
        }
    }
    
    @IBInspectable public var cornerRadiuss: CGFloat = 0 {
        didSet {
            cornerRadius = cornerRadiuss
        }
    }
    
    public var shadow: Shadow = Shadows.tokenShadowNormal {
        didSet {
            appearance()
        }
    }
        
    private var contentView: UIView?
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        appearance()
    }
    
    private func appearance() {
        layer.cornerRadius = cornerRadiusValue()
        layer.masksToBounds = false
        layer.shadowColor = shadowToken().color?.cgColor
        layer.shadowOffset = shadowToken().offset
        layer.shadowOpacity = shadowToken().opacity
        layer.shadowPath = shadowPathValue()
    }
    
    func cornerRadiusValue() -> CGFloat {
        return cornerRadius
    }
    
    func shadowToken() -> Shadow {
        return shadow
    }
    
    func shadowPathValue() -> CGPath {
        let axisX = -shadowToken().spread
        let rect = bounds.insetBy(dx: axisX, dy: axisX)
        let shadowPath = UIBezierPath(roundedRect: rect, cornerRadius: cornerRadiusValue())
        return shadowPath.cgPath
    }
}
