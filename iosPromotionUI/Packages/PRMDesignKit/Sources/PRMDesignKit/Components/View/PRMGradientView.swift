//
//  PRMGradientView.swift
//  CoreUIKit
//
//  Created by Natariannn on 8/7/20.
//  Copyright © 2020 ViettelPay App Team. All rights reserved.
//

import UIKit

public enum GradientDirection: Int {
    case left = 0
    case right = 1
    case top = 2
    case bottom = 3
    case radialTopRight = 4
    case radialBottomLeft  = 5
}

@IBDesignable
public class PRMGradientView: PRMPassThroughView {
    
    private var gradientView: UIView = UIView()
    
    private var gradientLayer: CAGradientLayer = CAGradientLayer()
    
    public var startColor: UIColor = Colors.tokenViettelPayRed80 {
        didSet {
            updateColors()
        }
    }
    
    public var endColor: UIColor = Colors.tokenViettelPayRed100 {
        didSet {
            updateColors()
        }
    }
    
    @IBInspectable public var direction: Int = 0 {
        didSet {
            commonInit()
        }
    }
    
    public var gradientBackgroundColor: UIColor = .white {
        didSet {
            commonInit()
        }
    }
    
    @IBInspectable public var cornerRadius: CGFloat = 0.0 {
        didSet {
            commonInit()
        }
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        commonInit()
    }
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        commonInit()
    }
    
    public override func layoutSubviews() {
        super.layoutSubviews()
        
        if let gradient = self.gradientView.layer.sublayers?[0] as? CAGradientLayer {
            gradient.frame = self.bounds
        }
    }
    
    override public func draw(_ rect: CGRect) {
        super.draw(rect)
        updateColors()
        gradientLayer.frame = gradientView.bounds
        
        switch direction {
            
        case GradientDirection.left.rawValue:
            gradientLayer.startPoint = CGPoint(x: 0.0, y: 1.0)
            gradientLayer.endPoint = CGPoint(x: 1.0, y: 1.0)
            
        case GradientDirection.right.rawValue:
            gradientLayer.startPoint = CGPoint(x: 1.0, y: 1.0)
            gradientLayer.endPoint = CGPoint(x: 0.0, y: 1.0)
            
        case GradientDirection.top.rawValue:
            gradientLayer.startPoint = CGPoint(x: 1.0, y: 0.0)
            gradientLayer.endPoint = CGPoint(x: 1.0, y: 1.0)
            
        case GradientDirection.bottom.rawValue:
            gradientLayer.startPoint = CGPoint(x: 1.0, y: 1.0)
            gradientLayer.endPoint = CGPoint(x: 1.0, y: 0.0)
            
        case GradientDirection.radialTopRight.rawValue:
            gradientLayer.type = .radial
            gradientLayer.startPoint = CGPoint(x: 1.0, y: 0.0)
            gradientLayer.endPoint   = CGPoint(x: 0.0, y: 1.0)

        case GradientDirection.radialBottomLeft.rawValue:
            gradientLayer.type = .radial
            gradientLayer.startPoint = CGPoint(x: 0.0, y: 1.0)
            gradientLayer.endPoint   = CGPoint(x: 1.0, y: 0.0)
            
        default:
            gradientLayer.startPoint = CGPoint(x: 0.0, y: 1.0)
            gradientLayer.endPoint = CGPoint(x: 1.0, y: 1.0)
        }
        
        gradientLayer.locations = [0, 1]
    }
}

extension PRMGradientView {
    private func commonInit() {
        guard gradientView.superview == nil else {
            gradientView.backgroundColor = gradientBackgroundColor
            backgroundColor = gradientBackgroundColor
            gradientView.layer.cornerRadius = cornerRadius
            layer.cornerRadius = cornerRadius
            return
        }
        
        addSubview(gradientView)
        gradientView.translatesAutoresizingMaskIntoConstraints = false
        gradientView.leftAnchor.constraint(equalTo: self.leftAnchor).isActive = true
        gradientView.rightAnchor.constraint(equalTo: self.rightAnchor).isActive = true
        gradientView.bottomAnchor.constraint(equalTo: self.bottomAnchor).isActive = true
        gradientView.topAnchor.constraint(equalTo: self.topAnchor).isActive = true
        gradientView.backgroundColor = gradientBackgroundColor
        gradientView.layer.cornerRadius = cornerRadius
        sendSubviewToBack(gradientView)
        
        gradientView.clipsToBounds = true
        gradientView.layer.insertSublayer(gradientLayer, at: 0)
        
        backgroundColor = gradientBackgroundColor
        layer.cornerRadius = cornerRadius
    }
    
    private func updateColors() {
        gradientLayer.colors = [startColor.cgColor, endColor.cgColor]
    }
}
